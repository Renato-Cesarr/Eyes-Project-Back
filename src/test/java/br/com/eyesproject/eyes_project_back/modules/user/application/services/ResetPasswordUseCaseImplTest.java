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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseImplTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordUseCaseImpl resetPasswordUseCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("Should successfully reset user password when given a valid token")
    void shouldResetPasswordSuccessfullyWhenTokenIsValid() {
        // Arrange
        User user = UserFactory.createValidUser();
        AuthToken validToken = UserFactory.createValidResetToken(user);
        String rawToken = validToken.getToken();
        String newPassword = "new_secure_password";
        String encodedPassword = "encoded_new_secure_password";

        when(authTokenRepository.findByTokenAndType(rawToken, TokenType.RESET)).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);

        // Act
        resetPasswordUseCase.execute(rawToken, newPassword);

        // Assert
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(encodedPassword, savedUser.getPassword(), "User password should be updated with encoded value");

        verify(authTokenRepository, times(1)).deleteById(validToken.getId());
    }

    @Test
    @DisplayName("Should throw DomainException when token implies not found or invalid type")
    void shouldThrowDomainExceptionWhenTokenIsInvalidOrNotFound() {
        // Arrange
        String rawToken = "invalid_token_xyz";
        String newPassword = "new_secure_password";

        when(authTokenRepository.findByTokenAndType(rawToken, TokenType.RESET)).thenReturn(Optional.empty());

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> resetPasswordUseCase.execute(rawToken, newPassword));
        assertEquals("Link de redefinição inválido ou expirado", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(authTokenRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw DomainException and delete token when token is expired")
    void shouldThrowDomainExceptionAndCleanupWhenTokenIsExpired() {
        // Arrange
        User user = UserFactory.createValidUser();
        AuthToken expiredToken = UserFactory.createExpiredToken(user, TokenType.RESET);
        String rawToken = expiredToken.getToken();
        String newPassword = "new_secure_password";

        when(authTokenRepository.findByTokenAndType(rawToken, TokenType.RESET)).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> resetPasswordUseCase.execute(rawToken, newPassword));
        assertEquals("O link expirou. Solicite uma nova redefinição de senha.", exception.getMessage());

        // Verify cleanup happens
        verify(authTokenRepository, times(1)).deleteById(expiredToken.getId());
        
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
