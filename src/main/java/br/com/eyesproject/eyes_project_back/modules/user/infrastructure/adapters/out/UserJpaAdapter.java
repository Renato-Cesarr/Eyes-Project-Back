package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        return springDataUserRepository.findById(UUID.fromString(id))
                .map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
                .map(this::mapToDomain);
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return springDataUserRepository.existsByRole(role);
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
