package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupPasswordUseCaseImplTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SetupPasswordUseCaseImpl setupPasswordUseCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("Should successfully setup user password, activate account, and cleanup token")
    void shouldSetupPasswordAndActivateUserWhenTokenIsValid() {
        // Arrange
        User inactiveUser = UserFactory.createInactiveUser();
        AuthToken validToken = UserFactory.createValidSetupToken(inactiveUser);
        String rawToken = validToken.getToken();
        String newPassword = "new_secure_password";
        String encodedPassword = "encoded_new_secure_password";

        when(authTokenRepository.findByTokenAndTypeForUpdate(rawToken, TokenType.SETUP))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        setupPasswordUseCase.execute(rawToken, newPassword);

        // Assert
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertTrue(savedUser.getActive(), "User should be activated after setup");
        assertEquals(encodedPassword, savedUser.getPassword(), "User password should be updated with encoded value");

        verify(authTokenRepository, times(1)).deleteById(validToken.getId());
    }

    @Test
    @DisplayName("Should throw DomainException when token implies not found or invalid type")
    void shouldThrowDomainExceptionWhenTokenIsInvalidOrNotFound() {
        // Arrange
        String rawToken = "invalid_setup_token_xyz";
        String newPassword = "new_secure_password";

        when(authTokenRepository.findByTokenAndTypeForUpdate(rawToken, TokenType.SETUP))
                .thenReturn(Optional.empty());

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> setupPasswordUseCase.execute(rawToken, newPassword));
        assertEquals("Link de ativação inválido ou expirado", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(authTokenRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should reject an expired token with the same neutral response")
    void shouldRejectExpiredTokenWithNeutralResponse() {
        // Arrange
        User user = UserFactory.createInactiveUser();
        AuthToken expiredToken = UserFactory.createExpiredToken(user, TokenType.SETUP);
        String rawToken = expiredToken.getToken();
        String newPassword = "new_secure_password";

        when(authTokenRepository.findByTokenAndTypeForUpdate(rawToken, TokenType.SETUP))
                .thenReturn(Optional.of(expiredToken));

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> setupPasswordUseCase.execute(rawToken, newPassword));
        assertEquals("Link de ativação inválido ou expirado", exception.getMessage());

        verify(authTokenRepository, never()).deleteById(any());
        
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
