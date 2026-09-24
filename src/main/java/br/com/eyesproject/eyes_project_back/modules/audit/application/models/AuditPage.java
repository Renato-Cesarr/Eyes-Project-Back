package br.com.eyesproject.eyes_project_back.modules.audit.application.models;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;

import java.util.List;

public record AuditPage(
        List<AuditLog> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
