package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogActionServiceTest {

    @Mock AuditLogRepository repository;

    @Test
    void assignsTimestampWhenCallerDoesNotProvideOne() {
        AuditLog log = AuditLog.builder()
                .action(AuditAction.ACCESS_REQUEST_APPROVED)
                .actorUserId("actor")
                .targetType("ACCESS_REQUEST")
                .targetId("target")
                .build();
        var service = new LogActionService(
                repository,
                Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC)
        );

        service.execute(log);

        assertNotNull(log.getTimestamp());
        verify(repository).save(log);
    }
}
