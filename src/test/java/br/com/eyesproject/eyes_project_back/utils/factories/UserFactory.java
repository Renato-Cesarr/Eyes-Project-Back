package br.com.eyesproject.eyes_project_back.utils.factories;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test Data Builder / Factory for Domain Models.
 * Provides ready-to-use valid entities strictly for testing.
 */
public class UserFactory {

    public static User createValidUser() {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name("John Doe")
                .email("johndoe@test.com")
                .password("encoded_password")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static User createInactiveUser() {
        User user = createValidUser();
        user.setActive(false);
        user.setPassword(null);
        return user;
    }

    public static AuthToken createValidSetupToken(User user) {
        return AuthToken.builder()
                .id(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .user(user)
                .type(TokenType.SETUP)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AuthToken createValidResetToken(User user) {
        return AuthToken.builder()
                .id(UUID.randomUUID().toString())
                .token(UUID.randomUUID().toString())
                .user(user)
                .type(TokenType.RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AuthToken createExpiredToken(User user, TokenType type) {
        AuthToken token = type == TokenType.SETUP ? createValidSetupToken(user) : createValidResetToken(user);
        token.setExpiresAt(LocalDateTime.now().minusHours(1)); // Make it expired
        return token;
    }
}
