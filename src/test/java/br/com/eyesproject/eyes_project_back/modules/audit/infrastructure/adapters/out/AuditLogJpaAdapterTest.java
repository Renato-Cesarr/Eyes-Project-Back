package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuditLogJpaAdapterTest {

    private static final UUID LOG_ID = UUID.fromString("bcd99e50-da94-4d59-bc1f-e50424276331");
    private static final UUID ACTOR_ID = UUID.fromString("c6064a83-75dd-4ed8-923d-c204e259e8c7");
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 9, 21, 12, 0);

    @Mock
    SpringDataAuditLogRepository springDataRepository;

    @Test
    void mapsAndPersistsTheCompleteAuditEvent() {
        AuditLogJpaAdapter adapter = adapter();
        AuditLog log = AuditLog.builder()
                .id(LOG_ID.toString())
                .action(AuditAction.ACCESS_REQUEST_APPROVED)
                .actorUserId(ACTOR_ID.toString())
                .targetType("ACCESS_REQUEST")
                .targetId("request-id")
                .result(AuditResult.SUCCESS)
                .correlationId("request-123")
                .metadata(Map.of("decision", "APPROVED"))
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
        assertEquals(AuditResult.SUCCESS, entity.getResult());
        assertEquals("request-123", entity.getCorrelationId());
        assertEquals("{\"decision\":\"APPROVED\"}", entity.getMetadataJson());
        assertEquals(OCCURRED_AT, entity.getOccurredAt());
    }

    @Test
    void letsTheDatabaseGenerateTheIdAndSerializesMissingMetadataAsEmpty() {
        AuditLog log = AuditLog.builder()
                .action(AuditAction.USER_INVITED)
                .actorUserId(ACTOR_ID.toString())
                .targetType("USER")
                .targetId("user-id")
                .result(AuditResult.SUCCESS)
                .correlationId("request-new")
                .metadata(null)
                .timestamp(OCCURRED_AT)
                .build();

        adapter().save(log);

        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals("{}", captor.getValue().getMetadataJson());
    }

    @Test
    void mapsStoredEventsBackToTheDomainInRepositoryOrder() {
        AuditLogJpaAdapter adapter = adapter();
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .id(LOG_ID)
                .action(AuditAction.ACCESS_REQUEST_REJECTED)
                .actorUserId(ACTOR_ID)
                .targetType("ACCESS_REQUEST")
                .targetId("request-id")
                .result(AuditResult.FAILURE)
                .correlationId("request-456")
                .metadataJson("{\"failureCategory\":\"CONFLICT\"}")
                .occurredAt(OCCURRED_AT)
                .build();
        when(springDataRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.search(new AuditQuery(
                0, 20, ACTOR_ID.toString(), null, null, null, null
        ));

        assertEquals(1, result.content().size());
        AuditLog log = result.content().getFirst();
        assertEquals(LOG_ID.toString(), log.getId());
        assertEquals(AuditAction.ACCESS_REQUEST_REJECTED, log.getAction());
        assertEquals(ACTOR_ID.toString(), log.getActorUserId());
        assertEquals("ACCESS_REQUEST", log.getTargetType());
        assertEquals("request-id", log.getTargetId());
        assertEquals(AuditResult.FAILURE, log.getResult());
        assertEquals("request-456", log.getCorrelationId());
        assertEquals(Map.of("failureCategory", "CONFLICT"), log.getMetadata());
        assertEquals(OCCURRED_AT, log.getTimestamp());
    }

    @Test
    void mapsBlankStoredMetadataAsAnEmptyMap() {
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .id(LOG_ID)
                .action(AuditAction.USER_INVITED)
                .actorUserId(ACTOR_ID)
                .targetType("USER")
                .targetId("user-id")
                .result(AuditResult.SUCCESS)
                .correlationId("request-blank")
                .metadataJson(" ")
                .occurredAt(OCCURRED_AT)
                .build();
        when(springDataRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        AuditLog result = adapter().search(new AuditQuery(0, 20, null, null, null, null, null))
                .content().getFirst();

        assertEquals(Map.of(), result.getMetadata());
    }

    private AuditLogJpaAdapter adapter() {
        return new AuditLogJpaAdapter(springDataRepository);
    }
}
