package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditContextProvider;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogActionServiceTest {

    @Mock AuditLogRepository repository;
    @Mock AuditContextProvider contextProvider;

    @Test
    void assignsTimestampWhenCallerDoesNotProvideOne() {
        AuditLog log = AuditLog.builder()
                .action(AuditAction.ACCESS_REQUEST_APPROVED)
                .targetType("ACCESS_REQUEST")
                .targetId("target")
                .build();
        when(contextProvider.currentActorUserId()).thenReturn(Optional.of("actor"));
        when(contextProvider.currentCorrelationId()).thenReturn(Optional.of("request-123"));
        var service = service();

        service.execute(log);

        assertNotNull(log.getTimestamp());
        assertEquals("actor", log.getActorUserId());
        assertEquals("request-123", log.getCorrelationId());
        assertEquals(AuditResult.SUCCESS, log.getResult());
        verify(repository).save(log);
    }

    @Test
    void removesSensitiveMetadataBeforePersistence() {
        AuditLog log = AuditLog.builder()
                .action(AuditAction.USER_INVITED)
                .actorUserId("actor")
                .targetType(" user ")
                .targetId(" target ")
                .metadata(Map.of(
                        "role", "STUDENT",
                        "password", "never-store-this",
                        "invitationToken", "never-store-this"
                ))
                .build();

        service().execute(log);

        assertEquals(Map.of("role", "STUDENT"), log.getMetadata());
        assertFalse(log.getMetadata().containsValue("never-store-this"));
        assertEquals("USER", log.getTargetType());
        assertEquals("target", log.getTargetId());
    }

    private LogActionService service() {
        return new LogActionService(
                repository,
                contextProvider,
                Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC)
        );
    }
}
