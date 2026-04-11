package br.com.eyesproject.eyes_project_back.modules.user.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.CreateUserRequest;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.SetupPasswordRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private SetupPasswordUseCase setupPasswordUseCase;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 201 Created on valid request")
    void shouldCreateUserSuccessfully() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Renato", "renato@test.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(createUserUseCase, times(1)).execute(any());
    }

    @Test
    @DisplayName("POST /api/v1/users/setup-password - Should return 200 OK on valid request")
    void shouldSetupPasswordSuccessfully() throws Exception {
        SetupPasswordRequest request = new SetupPasswordRequest("token123", "secret123");

        mockMvc.perform(post("/api/v1/users/setup-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(setupPasswordUseCase, times(1)).execute("token123", "secret123");
    }
}
