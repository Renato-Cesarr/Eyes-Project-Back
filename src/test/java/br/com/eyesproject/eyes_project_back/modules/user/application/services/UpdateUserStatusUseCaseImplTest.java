package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserStatusUseCaseImplTest {

    private static final String USER_ID = "bf4b6797-750d-4f5d-8277-11c9099a428c";

    @Mock UserRepository userRepository;
    @InjectMocks UpdateUserStatusUseCaseImpl useCase;

    @Test
    void shouldDeactivateStudent() {
        User student = user(UserRole.STUDENT, true, "hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(student));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        var result = useCase.execute(USER_ID, false);

        assertFalse(result.active());
        verify(userRepository).save(student);
    }

    @Test
    void shouldRejectDeactivationOfLastActiveAdmin() {
        User admin = user(UserRole.ADMIN, true, "hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(userRepository.countActiveByRoleForUpdate(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> useCase.execute(USER_ID, false));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectManualActivationOfPendingInvitation() {
        User invited = user(UserRole.STUDENT, false, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(invited));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(invited));

        assertThrows(ConflictException.class, () -> useCase.execute(USER_ID, true));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnImmediatelyWhenStatusIsAlreadyCorrect() {
        User activeStudent = user(UserRole.STUDENT, true, "hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeStudent));

        var result = useCase.execute(USER_ID, true);

        assertTrue(result.active());
        verify(userRepository, never()).findByIdForUpdate(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateAdminWhenAnotherActiveAdminExists() {
        User admin = user(UserRole.ADMIN, true, "hash");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));
        when(userRepository.countActiveByRoleForUpdate(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(admin));
        when(userRepository.save(admin)).thenReturn(admin);

        assertFalse(useCase.execute(USER_ID, false).active());
    }

    @Test
    void shouldReturnNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(USER_ID, false));
    }

    private User user(UserRole role, boolean active, String password) {
        return User.builder().id(USER_ID).name("Ana").email("ana@example.com")
                .role(role).active(active).password(password).build();
    }
}
