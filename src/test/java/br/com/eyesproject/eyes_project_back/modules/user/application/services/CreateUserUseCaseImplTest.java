package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.audit.application.services.AdministrativeAudit;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationIssuer invitationIssuer;

    @Mock
    private AdministrativeAudit administrativeAudit;

    @InjectMocks
    private CreateUserUseCaseImpl createUserUseCase;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("Should successfully create an inactive user, generate setup token and send email")
    void shouldCreateUserAndSendInvitationEmailSuccessfully() {
        // Arrange
        User requestUser = UserFactory.createValidUser();
        // Since it's a new user creation, active must be false and password null
        
        when(userRepository.findByEmail(requestUser.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User createdUser = createUserUseCase.execute(requestUser);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUserArg = userCaptor.getValue();
        
        // Ensure Domain rules applied:
        assertFalse(savedUserArg.getActive(), "User should be inactive immediately after creation");
        assertNull(savedUserArg.getPassword(), "User password should be null pending setup");
        assertEquals(UserRole.STUDENT, savedUserArg.getRole());
        assertEquals(requestUser.getEmail(), savedUserArg.getEmail());

        verify(invitationIssuer).issue(savedUserArg);
        verify(administrativeAudit).success(
                eq(AuditAction.USER_INVITED), isNull(), eq("USER"), any(), anyMap()
        );

        assertNotNull(createdUser);
    }

    @Test
    @DisplayName("Should throw ConflictException when trying to create a user with an existing email")
    void shouldThrowConflictExceptionWhenEmailAlreadyExists() {
        // Arrange
        User requestUser = UserFactory.createValidUser();

        when(userRepository.findByEmail(requestUser.getEmail())).thenReturn(Optional.of(requestUser));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> createUserUseCase.execute(requestUser));
        assertEquals("Este e-mail já está cadastrado no sistema", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(invitationIssuer);
        verify(administrativeAudit).failure(
                eq(AuditAction.USER_INVITED), isNull(), eq("USER"), isNull(), any(ConflictException.class)
        );
    }
}
