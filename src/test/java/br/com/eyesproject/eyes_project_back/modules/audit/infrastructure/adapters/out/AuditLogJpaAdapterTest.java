package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogJpaAdapterTest {

    private static final UUID LOG_ID = UUID.fromString("bcd99e50-da94-4d59-bc1f-e50424276331");
    private static final UUID ACTOR_ID = UUID.fromString("c6064a83-75dd-4ed8-923d-c204e259e8c7");
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 9, 21, 12, 0);

    @Mock
    SpringDataAuditLogRepository springDataRepository;

    @Test
    void mapsAndPersistsTheCompleteAuditEvent() {
        AuditLogJpaAdapter adapter = new AuditLogJpaAdapter(springDataRepository);
        AuditLog log = AuditLog.builder()
                .id(LOG_ID.toString())
                .action(AuditAction.ACCESS_REQUEST_APPROVED)
                .actorUserId(ACTOR_ID.toString())
                .targetType("ACCESS_REQUEST")
                .targetId("request-id")
                .timestamp(OCCURRED_AT)
                .build();

        adapter.save(log);

        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(springDataRepository).save(captor.capture());
        AuditLogJpaEntity entity = captor.getValue();
        assertEquals(LOG_ID, entity.getId());
        assertEquals(AuditAction.ACCESS_REQUEST_APPROVED, entity.getAction());
        assertEquals(ACTOR_ID, entity.getActorUserId());
        assertEquals("ACCESS_REQUEST", entity.getTargetType());
        assertEquals("request-id", entity.getTargetId());
        assertEquals(OCCURRED_AT, entity.getOccurredAt());
    }

    @Test
    void mapsStoredEventsBackToTheDomainInRepositoryOrder() {
        AuditLogJpaAdapter adapter = new AuditLogJpaAdapter(springDataRepository);
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .id(LOG_ID)
                .action(AuditAction.ACCESS_REQUEST_REJECTED)
                .actorUserId(ACTOR_ID)
                .targetType("ACCESS_REQUEST")
                .targetId("request-id")
                .occurredAt(OCCURRED_AT)
                .build();
        when(springDataRepository.findByActorUserIdOrderByOccurredAtDesc(ACTOR_ID))
                .thenReturn(List.of(entity));

        List<AuditLog> result = adapter.findByActorUserId(ACTOR_ID.toString());

        assertEquals(1, result.size());
        AuditLog log = result.getFirst();
        assertEquals(LOG_ID.toString(), log.getId());
        assertEquals(AuditAction.ACCESS_REQUEST_REJECTED, log.getAction());
        assertEquals(ACTOR_ID.toString(), log.getActorUserId());
        assertEquals("ACCESS_REQUEST", log.getTargetType());
        assertEquals("request-id", log.getTargetId());
        assertEquals(OCCURRED_AT, log.getTimestamp());
    }
}
