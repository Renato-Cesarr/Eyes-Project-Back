package br.com.eyesproject.eyes_project_back.modules.user.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.GetUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ResendInvitationUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SearchUsersUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.UpdateUserStatusUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

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

    @MockitoBean
    private SearchUsersUseCase searchUsersUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @MockitoBean
    private UpdateUserStatusUseCase updateUserStatusUseCase;

    @MockitoBean
    private ResendInvitationUseCase resendInvitationUseCase;

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

    @Test
    void shouldListUsersWithPaginationMetadata() throws Exception {
        UserView user = view(true, false);
        when(searchUsersUseCase.execute(any())).thenReturn(new PageResult<>(List.of(user), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnUserDetails() throws Exception {
        when(getUserUseCase.execute("user-id")).thenReturn(view(false, true));

        mockMvc.perform(get("/api/v1/users/user-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationPending").value(true));
    }

    @Test
    void shouldUpdateUserStatus() throws Exception {
        when(updateUserStatusUseCase.execute("user-id", false)).thenReturn(view(false, false));

        mockMvc.perform(patch("/api/v1/users/user-id/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldRejectMissingStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/users/user-id/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldResendInvitation() throws Exception {
        mockMvc.perform(post("/api/v1/users/user-id/resend-invitation"))
                .andExpect(status().isNoContent());
        verify(resendInvitationUseCase).execute("user-id");
    }

    private UserView view(boolean active, boolean invitationPending) {
        return new UserView("user-id", "Ana", "ana@example.com", UserRole.STUDENT,
                active, invitationPending, null, null);
    }
}
