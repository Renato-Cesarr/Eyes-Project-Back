package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.LogActionUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditContextProvider;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdministrativeAudit {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeAudit.class);
    private static final String UNRESOLVED_TARGET = "UNRESOLVED";
    private final LogActionUseCase logActionUseCase;
    private final AuditContextProvider auditContextProvider;

    public void success(
            AuditAction action,
            String actorUserId,
            String targetType,
            String targetId,
            Map<String, String> metadata
    ) {
        AuditLog event = event(
                action,
                actorUserId,
                targetType,
                targetId,
                AuditResult.SUCCESS,
                metadata
        );
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            logActionUseCase.execute(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    logActionUseCase.execute(event);
                } catch (RuntimeException auditFailure) {
                    // The business transaction is already committed. Returning an error here
                    // would falsely tell the client that the operation failed.
                    LOGGER.error(
                            "Não foi possível persistir evento administrativo após o commit: action={}, correlationId={}",
                            event.getAction(),
                            event.getCorrelationId()
                    );
                }
            }
        });
    }

    public void failure(
            AuditAction action,
            String actorUserId,
            String targetType,
            String targetId,
            RuntimeException failure
    ) {
        try {
            logActionUseCase.execute(event(
                    action,
                    actorUserId,
                    targetType,
                    targetId,
                    AuditResult.FAILURE,
                    Map.of("failureCategory", classify(failure))
            ));
        } catch (RuntimeException auditFailure) {
            failure.addSuppressed(auditFailure);
        }
    }

    private AuditLog event(
            AuditAction action,
            String actorUserId,
            String targetType,
            String targetId,
            AuditResult result,
            Map<String, String> metadata
    ) {
        String resolvedActorUserId = actorUserId == null || actorUserId.isBlank()
                ? auditContextProvider.currentActorUserId().orElse(null)
                : actorUserId;
        String correlationId = auditContextProvider.currentCorrelationId()
                .orElseGet(() -> UUID.randomUUID().toString());
        return AuditLog.builder()
                .action(action)
                .actorUserId(resolvedActorUserId)
                .targetType(targetType)
                .targetId(targetId == null || targetId.isBlank() ? UNRESOLVED_TARGET : targetId)
                .result(result)
                .correlationId(correlationId)
                .metadata(metadata == null ? Map.of() : metadata)
                .build();
    }

    private String classify(RuntimeException failure) {
        if (failure instanceof ConflictException) {
            return "CONFLICT";
        }
        if (failure instanceof ResourceNotFoundException) {
            return "NOT_FOUND";
        }
        if (failure instanceof DomainException) {
            return "DOMAIN_VALIDATION";
        }
        return "TECHNICAL_FAILURE";
    }
}
