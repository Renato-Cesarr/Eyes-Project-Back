package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.LogActionUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecideAccessRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    @Mock AccessRequestRepository accessRequestRepository;
    @Mock CreateUserUseCase createUserUseCase;
    @Mock LogActionUseCase logActionUseCase;

    private DecideAccessRequestService service;

    @BeforeEach
    void setUp() {
        service = new DecideAccessRequestService(
                accessRequestRepository,
                createUserUseCase,
                logActionUseCase,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("approves a pending request, creates one invitation and records audit")
    void approvesPendingRequest() {
        AccessRequest pending = pending();
        when(accessRequestRepository.findByIdForUpdate("request-id")).thenReturn(Optional.of(pending));
        when(accessRequestRepository.save(any(AccessRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, AccessRequest.class));

        AccessRequest result = service.execute("request-id", "admin-id");

        assertEquals(AccessRequestStatus.APPROVED, result.getStatus());
        assertEquals("admin-id", result.getDecidedByUserId());
        assertEquals(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), result.getDecidedAt());
        verify(createUserUseCase).execute(any());
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(logActionUseCase).execute(audit.capture());
        assertEquals(AuditAction.ACCESS_REQUEST_APPROVED, audit.getValue().getAction());
        assertEquals("request-id", audit.getValue().getTargetId());
    }

    @Test
    @DisplayName("does not issue another invitation when approval is repeated")
    void repeatedApprovalIsIdempotent() {
        AccessRequest approved = pending();
        approved.approve("first-admin", LocalDateTime.now());
        when(accessRequestRepository.findByIdForUpdate("request-id")).thenReturn(Optional.of(approved));

        AccessRequest result = service.execute("request-id", "second-admin");

        assertEquals("first-admin", result.getDecidedByUserId());
        verifyNoInteractions(createUserUseCase, logActionUseCase);
        verify(accessRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a pending request with a trimmed reason and audit record")
    void rejectsPendingRequest() {
        AccessRequest pending = pending();
        when(accessRequestRepository.findByIdForUpdate("request-id")).thenReturn(Optional.of(pending));
        when(accessRequestRepository.save(any(AccessRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, AccessRequest.class));

        AccessRequest result = service.execute("request-id", "admin-id", "  Dados insuficientes  ");

        assertEquals(AccessRequestStatus.REJECTED, result.getStatus());
        assertEquals("Dados insuficientes", result.getDecisionReason());
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(logActionUseCase).execute(audit.capture());
        assertEquals(AuditAction.ACCESS_REQUEST_REJECTED, audit.getValue().getAction());
    }

    @Test
    @DisplayName("rejects an empty reason even when the use case is called outside HTTP")
    void rejectsEmptyReasonAtApplicationBoundary() {
        AccessRequest pending = pending();
        when(accessRequestRepository.findByIdForUpdate("request-id")).thenReturn(Optional.of(pending));

        DomainException exception = assertThrows(
                DomainException.class,
                () -> service.execute("request-id", "admin-id", "   ")
        );

        assertEquals("Justificativa é obrigatória", exception.getMessage());
        verify(accessRequestRepository, never()).save(any());
        verifyNoInteractions(logActionUseCase);
    }

    @Test
    @DisplayName("does not allow reversing a final decision")
    void cannotReverseFinalDecision() {
        AccessRequest rejected = pending();
        rejected.reject("admin-id", "Motivo", LocalDateTime.now());
        when(accessRequestRepository.findByIdForUpdate("request-id")).thenReturn(Optional.of(rejected));

        assertThrows(ConflictException.class, () -> service.execute("request-id", "admin-id"));
        verifyNoInteractions(createUserUseCase, logActionUseCase);
    }

    @Test
    @DisplayName("returns not found for an unknown request")
    void unknownRequest() {
        when(accessRequestRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.execute("missing", "admin-id"));
    }

    private AccessRequest pending() {
        return AccessRequest.builder()
                .id("request-id")
                .name("Ana")
                .email("ana@example.com")
                .status(AccessRequestStatus.PENDING)
                .build();
    }
}
