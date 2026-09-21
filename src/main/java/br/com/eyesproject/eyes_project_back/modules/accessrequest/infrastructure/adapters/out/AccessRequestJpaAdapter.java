package br.com.eyesproject.eyes_project_back.modules.accessrequest.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestPage;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestQuery;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
@RequiredArgsConstructor
public class AccessRequestJpaAdapter implements AccessRequestRepository {

    private final SpringDataAccessRequestRepository springDataRepository;

    @Override
    public AccessRequest save(AccessRequest request) {
        try {
            return mapToDomain(springDataRepository.saveAndFlush(mapToEntity(request)));
        } catch (DataIntegrityViolationException exception) {
            if (request.getId() == null && request.getStatus() == AccessRequestStatus.PENDING) {
                throw new ConflictException("Já existe uma solicitação pendente para este e-mail", exception);
            }
            throw exception;
        }
    }

    @Override
    public Optional<AccessRequest> findById(String id) {
        return parseId(id).flatMap(springDataRepository::findById).map(this::mapToDomain);
    }

    @Override
    public Optional<AccessRequest> findByIdForUpdate(String id) {
        return parseId(id).flatMap(springDataRepository::findByIdForUpdate).map(this::mapToDomain);
    }

    @Override
    public Optional<AccessRequest> findPendingByEmail(String normalizedEmail) {
        return springDataRepository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                normalizedEmail,
                AccessRequestStatus.PENDING
        ).map(this::mapToDomain);
    }

    @Override
    public AccessRequestPage search(AccessRequestQuery query) {
        PageRequest pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        var specification = (org.springframework.data.jpa.domain.Specification<AccessRequestJpaEntity>)
                (root, ignored, builder) -> {
                    var predicates = new ArrayList<Predicate>();
                    if (query.search() != null) {
                        String pattern = "%" + query.search().toLowerCase() + "%";
                        predicates.add(builder.or(
                                builder.like(builder.lower(root.get("name")), pattern),
                                builder.like(builder.lower(root.get("email")), pattern)
                        ));
                    }
                    if (query.status() != null) {
                        predicates.add(builder.equal(root.get("status"), query.status()));
                    }
                    return builder.and(predicates.toArray(Predicate[]::new));
                };

        var page = springDataRepository.findAll(specification, pageable);
        return new AccessRequestPage(
                page.getContent().stream().map(this::mapToDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private Optional<UUID> parseId(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private AccessRequestJpaEntity mapToEntity(AccessRequest request) {
        return AccessRequestJpaEntity.builder()
                .id(request.getId() == null ? null : UUID.fromString(request.getId()))
                .name(request.getName())
                .email(request.getEmail())
                .requestReason(request.getRequestReason())
                .status(request.getStatus())
                .decisionReason(request.getDecisionReason())
                .decidedByUserId(request.getDecidedByUserId() == null
                        ? null
                        : UUID.fromString(request.getDecidedByUserId()))
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }

    private AccessRequest mapToDomain(AccessRequestJpaEntity entity) {
        return AccessRequest.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .email(entity.getEmail())
                .requestReason(entity.getRequestReason())
                .status(entity.getStatus())
                .decisionReason(entity.getDecisionReason())
                .decidedByUserId(entity.getDecidedByUserId() == null
                        ? null
                        : entity.getDecidedByUserId().toString())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .decidedAt(entity.getDecidedAt())
                .build();
    }
}
