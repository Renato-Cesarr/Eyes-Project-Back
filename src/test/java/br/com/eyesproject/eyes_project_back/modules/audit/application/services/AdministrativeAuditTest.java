package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.LogActionUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditContextProvider;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministrativeAuditTest {

    @Mock LogActionUseCase logActionUseCase;
    @Mock AuditContextProvider auditContextProvider;

    @Test
    void createsExplicitSuccessEvent() {
        AdministrativeAudit audit = audit();

        audit.success(
                AuditAction.USER_ACTIVATED,
                "actor-id",
                "USER",
                "user-id",
                Map.of("active", "true")
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(logActionUseCase).execute(captor.capture());
        AuditLog event = captor.getValue();
        assertEquals(AuditResult.SUCCESS, event.getResult());
        assertEquals("actor-id", event.getActorUserId());
        assertEquals("user-id", event.getTargetId());
        assertEquals(Map.of("active", "true"), event.getMetadata());
    }

    @Test
    void capturesRequestContextBeforeDeferringTheEvent() {
        when(auditContextProvider.currentActorUserId()).thenReturn(Optional.of("context-actor"));
        when(auditContextProvider.currentCorrelationId()).thenReturn(Optional.of("request-123"));
        AdministrativeAudit audit = audit();

        audit.success(AuditAction.USER_INVITED, null, "USER", "user-id", Map.of());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(logActionUseCase).execute(captor.capture());
        assertEquals("context-actor", captor.getValue().getActorUserId());
        assertEquals("request-123", captor.getValue().getCorrelationId());
    }

    @Test
    void classifiesFailureWithoutPersistingTheExceptionMessage() {
        AdministrativeAudit audit = audit();

        audit.failure(
                AuditAction.USER_DEACTIVATED,
                "actor-id",
                "USER",
                null,
                new ConflictException("mensagem interna que não pode ser persistida")
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(logActionUseCase).execute(captor.capture());
        AuditLog event = captor.getValue();
        assertEquals(AuditResult.FAILURE, event.getResult());
        assertEquals("UNRESOLVED", event.getTargetId());
        assertEquals(Map.of("failureCategory", "CONFLICT"), event.getMetadata());
        assertFalse(event.getMetadata().toString().contains("mensagem interna"));
    }

    @Test
    void neverMasksTheOriginalBusinessFailureWhenAuditStorageFails() {
        RuntimeException original = new RuntimeException("business failure");
        RuntimeException auditFailure = new RuntimeException("audit unavailable");
        doThrow(auditFailure).when(logActionUseCase).execute(any());
        AdministrativeAudit audit = audit();

        audit.failure(AuditAction.USER_INVITED, null, "USER", null, original);

        assertEquals(1, original.getSuppressed().length);
        assertSame(auditFailure, original.getSuppressed()[0]);
    }

    @Test
    void defersSuccessEventUntilTheBusinessTransactionCommits() {
        AdministrativeAudit audit = audit();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            audit.success(AuditAction.USER_INVITED, "actor", "USER", "user", Map.of());
            verifyNoInteractions(logActionUseCase);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(logActionUseCase).execute(any());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotReportACommittedOperationAsFailedWhenAuditStorageIsUnavailable() {
        doThrow(new RuntimeException("audit unavailable")).when(logActionUseCase).execute(any());
        AdministrativeAudit audit = audit();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            audit.success(AuditAction.USER_INVITED, "actor", "USER", "user", Map.of());

            assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit));
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void classifiesNotFoundValidationAndTechnicalFailures() {
        AdministrativeAudit audit = audit();
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        audit.failure(AuditAction.USER_INVITED, "actor", "USER", "one",
                new ResourceNotFoundException("not found"));
        audit.failure(AuditAction.USER_INVITED, "actor", "USER", "two",
                new DomainException("invalid"));
        audit.failure(AuditAction.USER_INVITED, "actor", "USER", "three",
                new IllegalStateException("technical"));

        verify(logActionUseCase, org.mockito.Mockito.times(3)).execute(captor.capture());
        assertEquals(
                java.util.List.of("NOT_FOUND", "DOMAIN_VALIDATION", "TECHNICAL_FAILURE"),
                captor.getAllValues().stream()
                        .map(event -> event.getMetadata().get("failureCategory"))
                        .toList()
        );
    }

    private AdministrativeAudit audit() {
        return new AdministrativeAudit(logActionUseCase, auditContextProvider);
    }
}
