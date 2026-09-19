package br.com.eyesproject.eyes_project_back.modules.user.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;

import java.util.Optional;

public interface AuthTokenRepository {
    AuthToken save(AuthToken token);
    Optional<AuthToken> findByTokenAndType(String token, TokenType type);
    Optional<AuthToken> findByTokenAndTypeForUpdate(String token, TokenType type);
    void deleteById(String id);
    void deleteByUserIdAndType(String userId, TokenType type);
}
