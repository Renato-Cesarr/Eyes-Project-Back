package br.com.eyesproject.eyes_project_back.modules.audit.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.SearchAuditLogsUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import br.com.eyesproject.eyes_project_back.modules.audit.presentation.dto.AuditPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Driving Adapter (Presentation). Exposes REST endpoints.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Consulta administrativa da trilha imutável de auditoria")
public class AuditController {

    private final SearchAuditLogsUseCase searchAuditLogsUseCase;

    @GetMapping
    @Operation(
            summary = "Consultar auditoria",
            description = "Lista eventos administrativos em ordem decrescente. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public AuditPageResponse search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredTo
    ) {
        return AuditPageResponse.from(searchAuditLogsUseCase.execute(new AuditQuery(
                page,
                size,
                actorUserId,
                action,
                result,
                occurredFrom,
                occurredTo
        )));
    }
}
