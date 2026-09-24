package br.com.eyesproject.eyes_project_back.modules.audit.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.SearchAuditLogsUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired WebApplicationContext context;
    @MockitoBean SearchAuditLogsUseCase searchAuditLogsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listsAuditEventsWithFiltersAndPaginationMetadata() throws Exception {
        String actorUserId = UUID.randomUUID().toString();
        AuditLog event = AuditLog.builder()
                .id("event-id")
                .action(AuditAction.USER_DEACTIVATED)
                .actorUserId(actorUserId)
                .targetType("USER")
                .targetId("user-id")
                .result(AuditResult.SUCCESS)
                .correlationId("request-123")
                .metadata(Map.of("active", "false"))
                .timestamp(LocalDateTime.of(2026, 9, 24, 12, 0))
                .build();
        when(searchAuditLogsUseCase.execute(argThat(query -> query.page() == 1)))
                .thenReturn(new AuditPage(List.of(event), 1, 20, 21, 2));

        mockMvc.perform(get("/api/v1/audit")
                        .param("page", "1")
                        .param("actorUserId", actorUserId)
                        .param("action", "USER_DEACTIVATED")
                        .param("result", "SUCCESS")
                        .param("occurredFrom", "2026-09-01T00:00:00")
                        .param("occurredTo", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("USER_DEACTIVATED"))
                .andExpect(jsonPath("$.content[0].result").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].metadata.active").value("false"))
                .andExpect(jsonPath("$.content[0].correlationId").value("request-123"))
                .andExpect(jsonPath("$.totalElements").value(21));

        verify(searchAuditLogsUseCase).execute(argThat(query ->
                query.page() == 1
                        && actorUserId.equals(query.actorUserId())
                        && query.action() == AuditAction.USER_DEACTIVATED
                        && query.result() == AuditResult.SUCCESS
                        && query.occurredFrom() != null
                        && query.occurredTo() != null
        ));
    }

    @Test
    void rejectsInvalidPaginationAtTheApplicationBoundary() throws Exception {
        mockMvc.perform(get("/api/v1/audit").param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/audit").param("actorUserId", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
