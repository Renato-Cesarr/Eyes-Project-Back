package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthTokenJpaAdapter implements AuthTokenRepository {

    private final SpringDataAuthTokenRepository repository;

    @Override
    public AuthToken save(AuthToken token) {
        AuthTokenJpaEntity entity = mapToEntity(token);
        AuthTokenJpaEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<AuthToken> findByTokenAndType(String token, TokenType type) {
        return repository.findByTokenAndType(token, type).map(this::mapToDomain);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(UUID.fromString(id));
    }

    @Override
    public void deleteByUserIdAndType(String userId, TokenType type) {
        repository.deleteByUserIdAndType(UUID.fromString(userId), type);
    }

    private AuthTokenJpaEntity mapToEntity(AuthToken token) {
        UserJpaEntity userEntity = UserJpaEntity.builder()
                .id(UUID.fromString(token.getUser().getId()))
                .build();

        return AuthTokenJpaEntity.builder()
                .id(token.getId() != null ? UUID.fromString(token.getId()) : null)
                .user(userEntity)
                .token(token.getToken())
                .type(token.getType())
                .expiresAt(token.getExpiresAt())
                .createdAt(token.getCreatedAt())
                .build();
    }

    private AuthToken mapToDomain(AuthTokenJpaEntity entity) {
        User user = User.builder()
                .id(entity.getUser().getId().toString())
                .name(entity.getUser().getName())
                .email(entity.getUser().getEmail())
                .role(entity.getUser().getRole())
                .build();

        return AuthToken.builder()
                .id(entity.getId().toString())
                .user(user)
                .token(entity.getToken())
                .type(entity.getType())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
