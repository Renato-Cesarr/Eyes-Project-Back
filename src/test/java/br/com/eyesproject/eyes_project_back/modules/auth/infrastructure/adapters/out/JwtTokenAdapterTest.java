package br.com.eyesproject.eyes_project_back.modules.auth.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import com.auth0.jwt.JWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenAdapterTest {

    private JwtTokenAdapter jwtTokenAdapter;
    private final String secret = "test-secret-key-12345678901234567890";

    @BeforeEach
    void setUp() {
        jwtTokenAdapter = new JwtTokenAdapter();
        ReflectionTestUtils.setField(jwtTokenAdapter, "secret", secret);
        ReflectionTestUtils.setField(jwtTokenAdapter, "expirationHours", 2);
    }

    @Test
    @DisplayName("Should generate a valid JWT token for a user")
    void shouldGenerateToken() {
        // Arrange
        User user = UserFactory.createValidUser();

        // Act
        String token = jwtTokenAdapter.generateToken(user);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(user.getRole().name(), JWT.decode(token).getClaim("role").asString());
    }

    @Test
    @DisplayName("Should validate a valid token and return the subject email")
    void shouldValidateValidToken() {
        // Arrange
        User user = UserFactory.createValidUser();
        String token = jwtTokenAdapter.generateToken(user);

        // Act
        String subject = jwtTokenAdapter.validateToken(token);

        // Assert
        assertEquals(user.getEmail(), subject);
    }

    @Test
    @DisplayName("Should return empty string for an invalid token")
    void shouldReturnEmptyStringForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.dummy.token";

        // Act
        String subject = jwtTokenAdapter.validateToken(invalidToken);

        // Assert
        assertEquals("", subject);
    }
}
