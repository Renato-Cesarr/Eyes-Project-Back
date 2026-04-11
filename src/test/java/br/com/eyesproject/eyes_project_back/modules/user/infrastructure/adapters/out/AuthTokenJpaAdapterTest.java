package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTokenJpaAdapterTest {

    @Mock
    private SpringDataAuthTokenRepository springDataAuthTokenRepository;

    @InjectMocks
    private AuthTokenJpaAdapter authTokenJpaAdapter;

    private User domainUser;
    private AuthToken domainToken;
    private AuthTokenJpaEntity tokenEntity;

    @BeforeEach
    void setUp() {
        domainUser = UserFactory.createValidUser();
        domainToken = UserFactory.createValidSetupToken(domainUser);
        
        tokenEntity = AuthTokenJpaEntity.builder()
                .id(UUID.fromString(domainToken.getId()))
                .user(UserJpaEntity.builder().id(UUID.fromString(domainUser.getId())).build())
                .token(domainToken.getToken())
                .type(domainToken.getType())
                .expiresAt(domainToken.getExpiresAt())
                .createdAt(domainToken.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("Should save AuthToken domain and return it mapped from entity")
    void shouldSaveTokenSuccessfully() {
        // Arrange
        when(springDataAuthTokenRepository.save(any(AuthTokenJpaEntity.class))).thenReturn(tokenEntity);

        // Act
        authTokenJpaAdapter.save(domainToken);

        // Assert
        verify(springDataAuthTokenRepository, times(1)).save(any(AuthTokenJpaEntity.class));
    }

    @Test
    @DisplayName("Should find token by raw string and type")
    void shouldFindTokenByStringAndType() {
        // Arrange
        when(springDataAuthTokenRepository.findByTokenAndType(domainToken.getToken(), TokenType.SETUP))
                .thenReturn(Optional.of(tokenEntity));

        // Act
        Optional<AuthToken> result = authTokenJpaAdapter.findByTokenAndType(domainToken.getToken(), TokenType.SETUP);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(domainToken.getToken(), result.get().getToken());
    }

    @Test
    @DisplayName("Should delete token by id")
    void shouldDeleteTokenById() {
        // Arrange
        String id = UUID.randomUUID().toString();

        // Act
        authTokenJpaAdapter.deleteById(id);

        // Assert
        verify(springDataAuthTokenRepository, times(1)).deleteById(UUID.fromString(id));
    }

    @Test
    @DisplayName("Should delete tokens by userId and type")
    void shouldDeleteByUserIdAndType() {
        // Arrange
        String userId = UUID.randomUUID().toString();

        // Act
        authTokenJpaAdapter.deleteByUserIdAndType(userId, TokenType.RESET);

        // Assert
        verify(springDataAuthTokenRepository, times(1)).deleteByUserIdAndType(UUID.fromString(userId), TokenType.RESET);
    }
}
