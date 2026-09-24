package br.com.eyesproject.eyes_project_back.modules.audit.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
        String id,
        AuditAction action,
        String actorUserId,
        String targetType,
        String targetId,
        AuditResult result,
        String correlationId,
        Map<String, String> metadata,
        LocalDateTime occurredAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getActorUserId(),
                log.getTargetType(),
                log.getTargetId(),
                log.getResult(),
                log.getCorrelationId(),
                log.getMetadata(),
                log.getTimestamp()
        );
    }
}
