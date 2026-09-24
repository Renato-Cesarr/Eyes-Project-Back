package br.com.eyesproject.eyes_project_back.modules.audit.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;

import java.util.List;

public record AuditPageResponse(
        List<AuditLogResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AuditPageResponse from(AuditPage result) {
        return new AuditPageResponse(
                result.content().stream().map(AuditLogResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
