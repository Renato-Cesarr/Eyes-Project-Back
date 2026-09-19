package br.com.eyesproject.eyes_project_back.modules.auth.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private LoginUseCaseImpl loginUseCase;

    @Test
    @DisplayName("Should successfully authenticate a user and return a token")
    void shouldLoginSuccessfullyAndReturnToken() {
        // Arrange
        User user = UserFactory.createValidUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "raw_password");
        String fakeToken = "ey12345.dummy.token";

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(tokenProvider.generateToken(user)).thenReturn(fakeToken);

        // Act
        LoginResponse response = loginUseCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals(fakeToken, response.getToken());
        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(user.getName(), response.getUser().getName());
        assertEquals(user.getEmail(), response.getUser().getEmail());
        assertEquals(user.getRole(), response.getUser().getRole());

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(passwordEncoder, times(1)).matches(request.getPassword(), user.getPassword());
        verify(tokenProvider, times(1)).generateToken(user);
    }

    @Test
    @DisplayName("Should throw DomainException when user email implies not found")
    void shouldThrowDomainExceptionWhenEmailNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest("nonexistent@test.com", "raw_password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> loginUseCase.execute(request));
        assertEquals("Dados de acesso inválidos", exception.getMessage());

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw DomainException when user is inactive")
    void shouldThrowDomainExceptionWhenUserIsInactive() {
        // Arrange
        User inactiveUser = UserFactory.createInactiveUser();
        LoginRequest request = new LoginRequest(inactiveUser.getEmail(), "raw_password");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> loginUseCase.execute(request));
        assertEquals("Conta de usuário desativada", exception.getMessage());

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw DomainException when password does not match")
    void shouldThrowDomainExceptionWhenPasswordIsInvalid() {
        // Arrange
        User user = UserFactory.createValidUser();
        LoginRequest request = new LoginRequest(user.getEmail(), "wrong_password");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        // Act & Assert
        DomainException exception = assertThrows(DomainException.class, () -> loginUseCase.execute(request));
        assertEquals("Dados de acesso inválidos", exception.getMessage());

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(passwordEncoder, times(1)).matches(request.getPassword(), user.getPassword());
        verify(tokenProvider, never()).generateToken(any());
    }
}
