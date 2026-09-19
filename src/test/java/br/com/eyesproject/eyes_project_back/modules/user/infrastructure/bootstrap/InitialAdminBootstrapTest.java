package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.bootstrap;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments arguments;

    @Test
    void createsFirstAdministratorFromExternalConfiguration() {
        InitialAdminBootstrap bootstrap = bootstrap(validProperties());
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("safe-password")).thenReturn("encoded-password");

        bootstrap.run(arguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("Administrator", saved.getName());
        assertEquals("admin@example.com", saved.getEmail());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals(UserRole.ADMIN, saved.getRole());
        assertTrue(saved.getActive());
    }

    @Test
    void skipsBootstrapWhenAnAdministratorAlreadyExists() {
        InitialAdminBootstrap bootstrap = bootstrap(validProperties());
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        bootstrap.run(arguments);

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void promotesAndActivatesExistingInvitedUser() {
        InitialAdminBootstrap bootstrap = bootstrap(validProperties());
        User invitedUser = UserFactory.createInactiveUser();
        invitedUser.setEmail("admin@example.com");
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(invitedUser));
        when(passwordEncoder.encode("safe-password")).thenReturn("encoded-password");

        bootstrap.run(arguments);

        assertEquals(UserRole.ADMIN, invitedUser.getRole());
        assertEquals("encoded-password", invitedUser.getPassword());
        assertTrue(invitedUser.getActive());
        verify(userRepository).save(invitedUser);
    }

    @Test
    void promotesActiveUserWithoutReplacingExistingPassword() {
        InitialAdminBootstrap bootstrap = bootstrap(validProperties());
        User activeUser = UserFactory.createValidUser();
        activeUser.setEmail("admin@example.com");
        String currentPassword = activeUser.getPassword();
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(activeUser));

        bootstrap.run(arguments);

        assertEquals(UserRole.ADMIN, activeUser.getRole());
        assertEquals(currentPassword, activeUser.getPassword());
        assertTrue(activeUser.getActive());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository).save(activeUser);
    }

    @Test
    void refusesEnabledBootstrapWhenARequiredValueIsMissing() {
        InitialAdminProperties invalid = new InitialAdminProperties(true, "", "admin@example.com", "safe-password");
        InitialAdminBootstrap bootstrap = bootstrap(invalid);

        assertThrows(IllegalStateException.class, () -> bootstrap.run(arguments));
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesEnabledBootstrapWithInvalidEmail() {
        InitialAdminProperties invalid = new InitialAdminProperties(true, "Administrator", "invalid", "safe-password");
        InitialAdminBootstrap bootstrap = bootstrap(invalid);

        assertThrows(IllegalStateException.class, () -> bootstrap.run(arguments));
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesEnabledBootstrapWithShortPassword() {
        InitialAdminProperties invalid = new InitialAdminProperties(true, "Administrator", "admin@example.com", "short");
        InitialAdminBootstrap bootstrap = bootstrap(invalid);

        assertThrows(IllegalStateException.class, () -> bootstrap.run(arguments));
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByRole(any());
    }

    private InitialAdminBootstrap bootstrap(InitialAdminProperties properties) {
        return new InitialAdminBootstrap(properties, userRepository, passwordEncoder);
    }

    private InitialAdminProperties validProperties() {
        return new InitialAdminProperties(
                true,
                "Administrator",
                " ADMIN@EXAMPLE.COM ",
                "safe-password"
        );
    }
}
