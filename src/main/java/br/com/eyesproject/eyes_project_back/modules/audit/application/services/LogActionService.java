package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.LogActionUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditContextProvider;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogActionService implements LogActionUseCase {

    private final AuditLogRepository auditLogRepository;
    private final AuditContextProvider auditContextProvider;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(AuditLog log) {
        Objects.requireNonNull(log, "O evento de auditoria é obrigatório");
        requireEventIdentity(log);
        if (log.getActorUserId() == null || log.getActorUserId().isBlank()) {
            log.setActorUserId(auditContextProvider.currentActorUserId()
                    .orElseThrow(() -> new IllegalStateException("Ator autenticado ausente na auditoria")));
        }
        if (log.getCorrelationId() == null || log.getCorrelationId().isBlank()) {
            log.setCorrelationId(auditContextProvider.currentCorrelationId()
                    .orElseGet(() -> UUID.randomUUID().toString()));
        }
        if (log.getResult() == null) {
            log.setResult(AuditResult.SUCCESS);
        }
        log.setTargetType(log.getTargetType().trim().toUpperCase(Locale.ROOT));
        log.setTargetId(log.getTargetId().trim());
        log.setMetadata(sanitizeMetadata(log.getMetadata()));
        if (log.getTimestamp() == null) {
            log.setTimestamp(LocalDateTime.now(clock));
        }
        auditLogRepository.save(log);
    }

    private void requireEventIdentity(AuditLog log) {
        if (log.getAction() == null
                || log.getTargetType() == null || log.getTargetType().isBlank()
                || log.getTargetId() == null || log.getTargetId().isBlank()) {
            throw new IllegalArgumentException("Ação, tipo e identificador do alvo são obrigatórios");
        }
    }

    private Map<String, String> sanitizeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> !containsForbiddenTerm(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(
                        entry -> limit(entry.getKey().trim(), 60),
                        entry -> limit(entry.getValue().trim(), 200),
                        (first, ignored) -> first
                ));
    }

    private boolean containsForbiddenTerm(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("password")
                || normalized.contains("senha")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("authorization")
                || normalized.contains("image")
                || normalized.contains("frame");
    }

    private String limit(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }
}
