package br.com.eyesproject.eyes_project_back.modules.user.presentation;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_IT", matches = "true")
class InvitationActivationPostgresIntegrationTest {

    private static final String ACTIVATION_PATH = "/api/v1/users/setup-password";
    private static final String NEUTRAL_ACTIVATION_ERROR = "Link de ativação inválido ou expirado";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSenderPort emailSenderPort;

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
    @DisplayName("Anonymous invitee activates an account once with a valid SETUP token")
    void anonymousInviteeCanActivateAccountOnlyOnce() throws Exception {
        User invitedUser = saveUser("Invitee", "invitee@example.com", UserRole.STUDENT, false);
        String setupToken = saveToken(invitedUser, TokenType.SETUP, LocalDateTime.now().plusHours(1));

        mockMvc.perform(post(ACTIVATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activationRequest(setupToken)))
                .andExpect(status().isOk());

        User activatedUser = userRepository.findByEmail(invitedUser.getEmail()).orElseThrow();
        assertTrue(activatedUser.getActive());
        assertTrue(passwordEncoder.matches("new-password", activatedUser.getPassword()));
        assertEquals(0, tokenCount(setupToken));

        mockMvc.perform(post(ACTIVATION_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activationRequest(setupToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(NEUTRAL_ACTIVATION_ERROR));
    }

    @Test
    @DisplayName("Invalid, expired and wrong-purpose tokens return the same safe response")
    void invalidTokensReturnANeutralResponse() throws Exception {
        User invitedUser = saveUser("Neutral Error", "neutral@example.com", UserRole.STUDENT, false);
        String expiredToken = saveToken(invitedUser, TokenType.SETUP, LocalDateTime.now().minusMinutes(1));
        String resetToken = saveToken(invitedUser, TokenType.RESET, LocalDateTime.now().plusMinutes(15));

        for (String token : List.of("unknown-token", expiredToken, resetToken)) {
            mockMvc.perform(post(ACTIVATION_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(activationRequest(token)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(NEUTRAL_ACTIVATION_ERROR));
        }

        User unchangedUser = userRepository.findByEmail(invitedUser.getEmail()).orElseThrow();
        assertFalse(unchangedUser.getActive());
        assertEquals(1, tokenCount(expiredToken));
        assertEquals(1, tokenCount(resetToken));
    }

    @Test
    @DisplayName("Only an ADMIN can create an invitation")
    void invitationCreationRequiresAdministratorRole() throws Exception {
        User admin = saveUser("Administrator", "admin@example.com", UserRole.ADMIN, true);
        User student = saveUser("Student", "student@example.com", UserRole.STUDENT, true);
        String adminJwt = tokenProvider.generateToken(admin);
        String studentJwt = tokenProvider.generateToken(student);
        String requestBody = """
                {"name":"Invited Student","email":"invited-student@example.com"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        User invitedUser = userRepository.findByEmail("invited-student@example.com").orElseThrow();
        assertFalse(invitedUser.getActive());
        assertEquals(UserRole.STUDENT, invitedUser.getRole());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSenderPort).sendInvitationEmail(
                eq("invited-student@example.com"), eq("Invited Student"), tokenCaptor.capture());
        assertEquals(1, tokenCount(tokenCaptor.getValue()));
    }

    @Test
    @DisplayName("Concurrent activation requests consume a SETUP token exactly once")
    void concurrentActivationConsumesTokenExactlyOnce() throws Exception {
        User invitedUser = saveUser("Concurrent Invitee", "concurrent@example.com", UserRole.STUDENT, false);
        String setupToken = saveToken(invitedUser, TokenType.SETUP, LocalDateTime.now().plusHours(1));
        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch startRequests = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                results.add(executor.submit(() -> {
                    workersReady.countDown();
                    startRequests.await();
                    MvcResult result = mockMvc.perform(post(ACTIVATION_PATH)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(activationRequest(setupToken)))
                            .andReturn();
                    return result.getResponse().getStatus();
                }));
            }

            workersReady.await();
            startRequests.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            Collections.sort(statuses);

            assertEquals(List.of(200, 400), statuses);
            assertEquals(0, tokenCount(setupToken));
            assertTrue(userRepository.findByEmail(invitedUser.getEmail()).orElseThrow().getActive());
        } finally {
            executor.shutdownNow();
        }
    }

    private User saveUser(String name, String email, UserRole role, boolean active) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password(active ? passwordEncoder.encode("existing-password") : null)
                .active(active)
                .role(role)
                .build());
    }

    private String saveToken(User user, TokenType type, LocalDateTime expiresAt) {
        String rawToken = UUID.randomUUID().toString();
        authTokenRepository.save(AuthToken.builder()
                .user(user)
                .token(rawToken)
                .type(type)
                .expiresAt(expiresAt)
                .build());
        return rawToken;
    }

    private int tokenCount(String rawToken) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_auth_tokens WHERE token = ?", Integer.class, rawToken);
        return count == null ? 0 : count;
    }

    private String activationRequest(String token) {
        return """
                {"token":"%s","password":"new-password"}
                """.formatted(token);
    }
}
