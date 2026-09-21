package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;

import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapToEntity(user);
        UserJpaEntity savedEntity = springDataUserRepository.save(entity);
        return mapToDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(String id) {
        return parseId(id).flatMap(springDataUserRepository::findById)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmailIgnoreCase(email.trim())
                .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByIdForUpdate(String id) {
        return parseId(id).flatMap(springDataUserRepository::findByIdForUpdate)
                .map(this::mapToDomain);
    }

    @Override
    public PageResult<User> search(UserQuery query) {
        Sort.Direction direction = query.direction() == UserQuery.SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, mapSortProperty(query.sortBy()));
        PageRequest pageable = PageRequest.of(query.page(), query.size(), sort);

        Specification<UserJpaEntity> specification = (root, ignored, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (query.search() != null) {
                String pattern = "%" + query.search().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern)
                ));
            }
            if (query.role() != null) {
                predicates.add(builder.equal(root.get("role"), query.role()));
            }
            if (query.active() != null) {
                predicates.add(builder.equal(root.get("active"), query.active()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        var result = springDataUserRepository.findAll(specification, pageable);
        return new PageResult<>(
                result.getContent().stream().map(this::mapToDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public long countActiveByRoleForUpdate(UserRole role) {
        return springDataUserRepository.findActiveByRoleForUpdate(role).size();
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return springDataUserRepository.existsByRole(role);
    }

    private String mapSortProperty(UserQuery.SortField sortField) {
        return switch (sortField) {
            case NAME -> "name";
            case EMAIL -> "email";
            case CREATED_AT -> "createdAt";
        };
    }

    private Optional<UUID> parseId(String id) {
        try {
            return Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private UserJpaEntity mapToEntity(User user) {
        return UserJpaEntity.builder()
                .id(user.getId() != null ? UUID.fromString(user.getId()) : null)
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .active(user.getActive() == null || user.getActive())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User mapToDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId().toString())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .active(entity.getActive())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
