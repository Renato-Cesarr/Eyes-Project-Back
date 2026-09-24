package br.com.eyesproject.eyes_project_back.modules.audit.application.models;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditQuery(
        int page,
        int size,
        String actorUserId,
        AuditAction action,
        AuditResult result,
        LocalDateTime occurredFrom,
        LocalDateTime occurredTo
) {
    public AuditQuery {
        if (page < 0) {
            throw new DomainException("A página não pode ser negativa");
        }
        if (size < 1 || size > 100) {
            throw new DomainException("O tamanho da página deve estar entre 1 e 100");
        }
        actorUserId = normalize(actorUserId);
        validateActorUserId(actorUserId);
        if (occurredFrom != null && occurredTo != null && occurredFrom.isAfter(occurredTo)) {
            throw new DomainException("O início do período não pode ser posterior ao fim");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateActorUserId(String actorUserId) {
        if (actorUserId == null) {
            return;
        }
        try {
            UUID.fromString(actorUserId);
        } catch (IllegalArgumentException exception) {
            throw new DomainException("O identificador do ator é inválido");
        }
    }
}
