package br.com.eyesproject.eyes_project_back.global.security;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.SearchAuditLogsUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class RbacSecurityIntegrationTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String STUDENT_TOKEN = "student-token";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private SetupPasswordUseCase setupPasswordUseCase;

    @MockitoBean
    private SearchAuditLogsUseCase searchAuditLogsUseCase;

    private MockMvc mockMvc;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        reset(tokenProvider, userRepository, createUserUseCase, setupPasswordUseCase,
                searchAuditLogsUseCase);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        admin = UserFactory.createAdminUser();
        student = UserFactory.createValidUser();
        student.setEmail("student-auth@example.com");
        when(tokenProvider.validateToken(ADMIN_TOKEN)).thenReturn(admin.getEmail());
        when(tokenProvider.validateToken(STUDENT_TOKEN)).thenReturn(student.getEmail());
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(searchAuditLogsUseCase.execute(any()))
                .thenReturn(new AuditPage(List.of(), 0, 20, 0, 0));
    }

    @Test
    @DisplayName("ADMIN can access administrative user endpoint")
    void adminCanAccessAdministrativeEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Student","email":"student@example.com"}
                                """))
                .andExpect(status().isCreated());

        verify(createUserUseCase).execute(any(User.class));
    }

    @Test
    @DisplayName("STUDENT receives 403 on administrative endpoint")
    void studentCannotAccessAdministrativeEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Student","email":"student@example.com"}
                                """))
                .andExpect(status().isForbidden());

        verify(createUserUseCase, never()).execute(any(User.class));
    }

    @Test
    @DisplayName("Anonymous user receives 401 on administrative endpoint")
    void anonymousCannotAccessAdministrativeEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Student","email":"student@example.com"}
                                """))
                .andExpect(status().isUnauthorized());

        verify(createUserUseCase, never()).execute(any(User.class));
    }

    @Test
    @DisplayName("Only ADMIN can consult audit events")
    void onlyAdminCanConsultAudit() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user can read identity and role from me endpoint")
    void authenticatedUserCanReadCurrentProfile() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.email").value(student.getEmail()))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("Inactive user cannot authenticate with a previously issued token")
    void inactiveUserCannotAuthenticate() throws Exception {
        student.setActive(false);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + STUDENT_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid bearer token does not create an authenticated session")
    void invalidTokenCannotAuthenticate() throws Exception {
        when(tokenProvider.validateToken("invalid-token")).thenReturn("");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invitation password setup remains public")
    void setupPasswordRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/users/setup-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"token123","password":"secret123"}
                                """))
                .andExpect(status().isOk());

        verify(setupPasswordUseCase).execute("token123", "secret123");
    }

    @Test
    @DisplayName("CORS never enables browser-managed credentials for bearer authentication")
    void corsDoesNotAllowBrowserManagedCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
