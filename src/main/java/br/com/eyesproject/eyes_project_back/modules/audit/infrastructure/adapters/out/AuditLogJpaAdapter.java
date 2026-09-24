package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
@RequiredArgsConstructor
public class AuditLogJpaAdapter implements AuditLogRepository {

    private static final String OCCURRED_AT_FIELD = "occurredAt";
    private final SpringDataAuditLogRepository springDataRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void save(AuditLog log) {
        springDataRepository.save(AuditLogJpaEntity.builder()
                .id(log.getId() == null ? null : UUID.fromString(log.getId()))
                .action(log.getAction())
                .actorUserId(UUID.fromString(log.getActorUserId()))
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .result(log.getResult())
                .correlationId(log.getCorrelationId())
                .metadataJson(writeMetadata(log.getMetadata()))
                .occurredAt(log.getTimestamp())
                .build());
    }

    @Override
    public AuditPage search(AuditQuery query) {
        Specification<AuditLogJpaEntity> specification = Specification.unrestricted();
        if (query.actorUserId() != null) {
            UUID actorId = UUID.fromString(query.actorUserId());
            specification = specification.and((root, ignored, builder) ->
                    builder.equal(root.get("actorUserId"), actorId));
        }
        if (query.action() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.equal(root.get("action"), query.action()));
        }
        if (query.result() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.equal(root.get("result"), query.result()));
        }
        if (query.occurredFrom() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.greaterThanOrEqualTo(root.get(OCCURRED_AT_FIELD), query.occurredFrom()));
        }
        if (query.occurredTo() != null) {
            specification = specification.and((root, ignored, builder) ->
                    builder.lessThanOrEqualTo(root.get(OCCURRED_AT_FIELD), query.occurredTo()));
        }

        var result = springDataRepository.findAll(
                specification,
                PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, OCCURRED_AT_FIELD))
        );
        return new AuditPage(
                result.getContent().stream().map(this::mapToDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private AuditLog mapToDomain(AuditLogJpaEntity entity) {
        return AuditLog.builder()
                .id(entity.getId().toString())
                .action(entity.getAction())
                .actorUserId(entity.getActorUserId().toString())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .result(entity.getResult())
                .correlationId(entity.getCorrelationId())
                .metadata(readMetadata(entity.getMetadataJson()))
                .timestamp(entity.getOccurredAt())
                .build();
    }

    private String writeMetadata(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Metadados de auditoria inválidos", exception);
        }
    }

    private Map<String, String> readMetadata(String metadataJson) {
        try {
            if (metadataJson == null || metadataJson.isBlank()) {
                return Map.of();
            }
            return Map.copyOf(objectMapper.readValue(
                    metadataJson,
                    new TypeReference<Map<String, String>>() { }
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Metadados de auditoria corrompidos", exception);
        }
    }
}
