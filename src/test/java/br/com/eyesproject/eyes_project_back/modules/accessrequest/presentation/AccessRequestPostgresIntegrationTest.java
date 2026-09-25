package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation;

import br.com.eyesproject.eyes_project_back.support.PostgresContainerIntegrationTest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.ApproveAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SubmitAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AccessRequestPostgresIntegrationTest extends PostgresContainerIntegrationTest {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserRepository userRepository;
    @Autowired AccessRequestRepository accessRequestRepository;
    @Autowired SubmitAccessRequestUseCase submitAccessRequestUseCase;
    @Autowired ApproveAccessRequestUseCase approveAccessRequestUseCase;
    @Autowired TokenProvider tokenProvider;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @MockitoBean EmailSenderPort emailSenderPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tb_audit_logs, tb_access_requests, tb_auth_tokens, tb_users CASCADE");
        reset(emailSenderPort);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("public submission is idempotent and never exposes request PII")
    void publicSubmissionIsIdempotent() throws Exception {
        String payload = """
                {"name":"Ana Silva","email":"ANA@example.com","reason":"Acompanhar as aulas"}
                """;

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/access-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.message").value("Solicitação recebida para análise"))
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        assertEquals(1, count("SELECT COUNT(*) FROM tb_access_requests WHERE email = 'ana@example.com'"));
    }

    @Test
    @DisplayName("registered emails cannot create an access request")
    void registeredEmailConflicts() throws Exception {
        saveUser("Existing", "existing@example.com", UserRole.STUDENT);

        mockMvc.perform(post("/api/v1/access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Existing","email":"EXISTING@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Não é possível criar uma solicitação para este e-mail"));

        assertEquals(0, count("SELECT COUNT(*) FROM tb_access_requests"));
    }

    @Test
    @DisplayName("only ADMIN can list and decide access requests")
    void administrativeEndpointsEnforceRbac() throws Exception {
        User admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN);
        User student = saveUser("Student", "student@example.com", UserRole.STUDENT);
        submit("requester@example.com");

        mockMvc.perform(get("/api/v1/access-requests"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/access-requests").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/access-requests").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("approval creates one invitation and one immutable audit decision")
    void approvalIsIdempotentAndAudited() throws Exception {
        User admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN);
        String requestId = submit("new.student@example.com").getId();

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/access-requests/{id}/approve", requestId)
                            .header("Authorization", bearer(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        User invited = userRepository.findByEmail("new.student@example.com").orElseThrow();
        assertFalse(invited.getActive());
        verify(emailSenderPort, times(1)).sendInvitationEmail(
                anyString(), anyString(), anyString());
        assertEquals(1, count("SELECT COUNT(*) FROM tb_audit_logs WHERE action = 'ACCESS_REQUEST_APPROVED'"));
        assertEquals(1, count("SELECT COUNT(*) FROM tb_auth_tokens WHERE type = 'SETUP'"));
    }

    @Test
    @DisplayName("rejection stores the reason without creating a user")
    void rejectionStoresReasonAndAudit() throws Exception {
        User admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN);
        String requestId = submit("rejected@example.com").getId();

        mockMvc.perform(post("/api/v1/access-requests/{id}/reject", requestId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Dados insuficientes"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionReason").value("Dados insuficientes"));

        assertEquals(0, count("SELECT COUNT(*) FROM tb_users WHERE email = 'rejected@example.com'"));
        assertEquals(1, count("SELECT COUNT(*) FROM tb_audit_logs WHERE action = 'ACCESS_REQUEST_REJECTED'"));
    }

    @Test
    @DisplayName("concurrent submissions and approvals remain single-shot")
    void concurrentOperationsRemainIdempotent() throws Exception {
        User admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN);
        runConcurrently(() -> submitAccessRequestUseCase.execute(AccessRequest.builder()
                .name("Concurrent Student")
                .email("concurrent@example.com")
                .build()));
        assertEquals(1, count("SELECT COUNT(*) FROM tb_access_requests WHERE email = 'concurrent@example.com'"));

        String requestId = accessRequestRepository.findPendingByEmail("concurrent@example.com").orElseThrow().getId();
        runConcurrently(() -> approveAccessRequestUseCase.execute(requestId, admin.getId()));

        assertEquals(1, count("SELECT COUNT(*) FROM tb_users WHERE email = 'concurrent@example.com'"));
        assertEquals(1, count("SELECT COUNT(*) FROM tb_audit_logs WHERE action = 'ACCESS_REQUEST_APPROVED'"));
        verify(emailSenderPort, times(1)).sendInvitationEmail(anyString(), anyString(), anyString());
    }

    private AccessRequest submit(String email) {
        return submitAccessRequestUseCase.execute(AccessRequest.builder()
                .name("New Student")
                .email(email)
                .build()).request();
    }

    private User saveUser(String name, String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .active(true)
                .role(role)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    private int count(String sql) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }

    private void runConcurrently(ThrowingOperation operation) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    operation.run();
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }
}
