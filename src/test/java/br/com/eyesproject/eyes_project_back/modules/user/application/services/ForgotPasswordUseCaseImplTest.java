package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private EmailSenderPort emailSenderPort;

    @InjectMocks
    private ForgotPasswordUseCaseImpl forgotPasswordUseCase;

    @Captor
    private ArgumentCaptor<AuthToken> tokenCaptor;

    @Test
    @DisplayName("Should generate a token, delete old tokens, and send recovery email when user is active")
    void shouldGenerateResetTokenAndSendEmailWhenUserIsActive() {
        // Arrange
        User user = UserFactory.createValidUser();
        String targetEmail = user.getEmail();

        when(userRepository.findByEmail(targetEmail)).thenReturn(Optional.of(user));

        // Act
        forgotPasswordUseCase.execute(targetEmail);

        // Assert
        verify(authTokenRepository, times(1)).deleteByUserIdAndType(user.getId(), TokenType.RESET);
        
        verify(authTokenRepository, times(1)).save(tokenCaptor.capture());
        AuthToken savedToken = tokenCaptor.getValue();
        assertEquals(user, savedToken.getUser());
        assertEquals(TokenType.RESET, savedToken.getType());
        assertNotNull(savedToken.getToken());

        verify(emailSenderPort, times(1)).sendPasswordResetEmail(user.getEmail(), user.getName(), savedToken.getToken());
    }

    @Test
    @DisplayName("Should fail silently (prevent enumeration) when user does not exist")
    void shouldDoNothingWhenUserNotFound() {
        // Arrange
        String targetEmail = "nonexistent@test.com";

        when(userRepository.findByEmail(targetEmail)).thenReturn(Optional.empty());

        // Act
        forgotPasswordUseCase.execute(targetEmail);

        // Assert
        verify(authTokenRepository, never()).deleteByUserIdAndType(anyString(), any());
        verify(authTokenRepository, never()).save(any(AuthToken.class));
        verify(emailSenderPort, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should fail silently (prevent enumeration) when user is inactive")
    void shouldDoNothingWhenUserIsInactive() {
        // Arrange
        User inactiveUser = UserFactory.createInactiveUser();
        String targetEmail = inactiveUser.getEmail();

        when(userRepository.findByEmail(targetEmail)).thenReturn(Optional.of(inactiveUser));

        // Act
        forgotPasswordUseCase.execute(targetEmail);

        // Assert
        verify(authTokenRepository, never()).deleteByUserIdAndType(anyString(), any());
        verify(authTokenRepository, never()).save(any(AuthToken.class));
        verify(emailSenderPort, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }
}
