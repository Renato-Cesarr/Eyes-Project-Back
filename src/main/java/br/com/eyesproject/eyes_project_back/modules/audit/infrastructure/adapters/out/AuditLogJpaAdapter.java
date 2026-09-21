package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
@RequiredArgsConstructor
public class AuditLogJpaAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository springDataRepository;

    @Override
    public void save(AuditLog log) {
        springDataRepository.save(AuditLogJpaEntity.builder()
                .id(log.getId() == null ? null : UUID.fromString(log.getId()))
                .action(log.getAction())
                .actorUserId(UUID.fromString(log.getActorUserId()))
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .occurredAt(log.getTimestamp())
                .build());
    }

    @Override
    public List<AuditLog> findByActorUserId(String userId) {
        return springDataRepository.findByActorUserIdOrderByOccurredAtDesc(UUID.fromString(userId))
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    private AuditLog mapToDomain(AuditLogJpaEntity entity) {
        return AuditLog.builder()
                .id(entity.getId().toString())
                .action(entity.getAction())
                .actorUserId(entity.getActorUserId().toString())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .timestamp(entity.getOccurredAt())
                .build();
    }
}
