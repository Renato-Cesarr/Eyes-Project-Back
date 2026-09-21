package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestPage;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestSubmission;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.ApproveAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.RejectAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SearchAccessRequestsUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SubmitAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AccessRequestControllerTest {

    @Autowired WebApplicationContext webApplicationContext;
    @MockitoBean SubmitAccessRequestUseCase submitAccessRequestUseCase;
    @MockitoBean SearchAccessRequestsUseCase searchAccessRequestsUseCase;
    @MockitoBean ApproveAccessRequestUseCase approveAccessRequestUseCase;
    @MockitoBean RejectAccessRequestUseCase rejectAccessRequestUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("public submission returns a neutral accepted response without PII")
    void acceptsPublicRequestWithoutExposingData() throws Exception {
        AccessRequest request = request();
        when(submitAccessRequestUseCase.execute(any()))
                .thenReturn(new AccessRequestSubmission(request, true));

        mockMvc.perform(post("/api/v1/access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ana Silva",
                                  "email": "ana@example.com",
                                  "reason": "Acompanhar as aulas"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Solicitação recebida para análise"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(submitAccessRequestUseCase).execute(any());
    }

    @Test
    @DisplayName("invalid public input returns 422")
    void validatesPublicRequest() throws Exception {
        mockMvc.perform(post("/api/v1/access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"invalid"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("administrative listing exposes pagination metadata")
    void listsRequestsWithPagination() throws Exception {
        when(searchAccessRequestsUseCase.execute(any()))
                .thenReturn(new AccessRequestPage(List.of(request()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/access-requests")
                        .param("status", "PENDING")
                        .param("search", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private AccessRequest request() {
        return AccessRequest.builder()
                .id("request-id")
                .name("Ana Silva")
                .email("ana@example.com")
                .requestReason("Acompanhar as aulas")
                .status(AccessRequestStatus.PENDING)
                .build();
    }
}
