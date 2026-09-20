package br.com.eyesproject.eyes_project_back.modules.user.presentation;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.UpdateUserStatusUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_IT", matches = "true")
class UserManagementPostgresIntegrationTest {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserRepository userRepository;
    @Autowired AuthTokenRepository authTokenRepository;
    @Autowired UpdateUserStatusUseCase updateUserStatusUseCase;
    @Autowired TokenProvider tokenProvider;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean EmailSenderPort emailSenderPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tb_auth_tokens, tb_users CASCADE");
        reset(emailSenderPort);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("ADMIN can paginate, filter and inspect users without sensitive fields")
    void adminCanSearchAndInspectUsersSafely() throws Exception {
        User admin = saveUser("Root Admin", "root@example.com", UserRole.ADMIN, true, true);
        saveUser("Ana Student", "ana@example.com", UserRole.STUDENT, true, true);
        saveUser("Pending Student", "pending@example.com", UserRole.STUDENT, false, false);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearer(admin))
                        .param("search", "student")
                        .param("role", "STUDENT")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());

        User pending = userRepository.findByEmail("pending@example.com").orElseThrow();
        mockMvc.perform(get("/api/v1/users/{id}", pending.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pending@example.com"))
                .andExpect(jsonPath("$.invitationPending").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Management endpoints return 401 to anonymous users and 403 to STUDENT")
    void managementEndpointsEnforceRbac() throws Exception {
        User student = saveUser("Student", "student@example.com", UserRole.STUDENT, true, true);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN can deactivate a student but cannot activate a pending invitation")
    void statusChangeEnforcesLifecycleRules() throws Exception {
        User admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN, true, true);
        User student = saveUser("Student", "student@example.com", UserRole.STUDENT, true, true);
        User invited = saveUser("Invited", "invited@example.com", UserRole.STUDENT, false, false);

        mockMvc.perform(patch("/api/v1/users/{id}/status", student.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/users/{id}/status", invited.getId())
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Resending an invitation revokes the previous token")
    void resendInvitationReplacesPreviousToken() throws Exception {
        User admin = saveUser("Admin", "admin@example.com", UserRole.ADMIN, true, true);
        User invited = saveUser("Invited", "invited@example.com", UserRole.STUDENT, false, false);
        String oldToken = saveSetupToken(invited);

        mockMvc.perform(post("/api/v1/users/{id}/resend-invitation", invited.getId())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());

        assertEquals(0, countToken(oldToken));
        ArgumentCaptor<String> newToken = ArgumentCaptor.forClass(String.class);
        verify(emailSenderPort).sendInvitationEmail(
                eq(invited.getEmail()), eq(invited.getName()), newToken.capture());
        assertEquals(1, countToken(newToken.getValue()));
    }

    @Test
    @DisplayName("Concurrent operations always preserve at least one active ADMIN")
    void concurrentDeactivationPreservesOneActiveAdmin() throws Exception {
        User first = saveUser("First Admin", "first@example.com", UserRole.ADMIN, true, true);
        User second = saveUser("Second Admin", "second@example.com", UserRole.ADMIN, true, true);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (String id : List.of(first.getId(), second.getId())) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        updateUserStatusUseCase.execute(id, false);
                        return true;
                    } catch (ConflictException exception) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();

            int successfulChanges = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successfulChanges++;
            }
            assertEquals(1, successfulChanges);
            assertEquals(1, countActiveAdmins());
        } finally {
            executor.shutdownNow();
        }
    }

    private User saveUser(String name, String email, UserRole role, boolean active, boolean hasPassword) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(hasPassword ? passwordEncoder.encode("existing-password") : null)
                .active(active)
                .role(role)
                .build());
    }

    private String saveSetupToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        authTokenRepository.save(AuthToken.builder()
                .user(user)
                .token(rawToken)
                .type(TokenType.SETUP)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());
        return rawToken;
    }

    private String bearer(User user) {
        return "Bearer " + tokenProvider.generateToken(user);
    }

    private int countToken(String token) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_auth_tokens WHERE token = ?", Integer.class, token);
        return count == null ? 0 : count;
    }

    private int countActiveAdmins() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_users WHERE role = 'ADMIN' AND active = true", Integer.class);
        return count == null ? 0 : count;
    }
}
