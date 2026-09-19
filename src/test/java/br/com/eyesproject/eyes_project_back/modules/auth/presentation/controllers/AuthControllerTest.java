package br.com.eyesproject.eyes_project_back.modules.auth.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in.LoginUseCase;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ForgotPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ResetPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private ForgotPasswordUseCase forgotPasswordUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Ensure Java 8 Dates etc work
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 200 OK and LoginResponse")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        LoginResponse response = LoginResponse.builder()
                .token("dummy-jwt-token")
                .user(LoginResponse.UserResponse.builder()
                        .id("123")
                        .name("Test User")
                        .email("test@test.com")
                        .role(UserRole.STUDENT)
                        .build())
                .build();

        when(loginUseCase.execute(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy-jwt-token"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 400 Bad Request on invalid input")
    void shouldReturnBadRequestOnInvalidLoginInput() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
