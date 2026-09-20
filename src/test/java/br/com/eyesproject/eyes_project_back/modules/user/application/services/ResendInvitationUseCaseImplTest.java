package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendInvitationUseCaseImplTest {

    private static final String USER_ID = "bf4b6797-750d-4f5d-8277-11c9099a428c";

    @Mock UserRepository userRepository;
    @Mock InvitationIssuer invitationIssuer;
    @InjectMocks ResendInvitationUseCaseImpl useCase;

    @Test
    void shouldResendPendingInvitation() {
        User invited = User.builder().id(USER_ID).active(false).password(null).build();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(invited));

        useCase.execute(USER_ID);

        verify(invitationIssuer).issue(invited);
    }

    @Test
    void shouldRejectFormerUserThatWasDeactivated() {
        User deactivated = User.builder().id(USER_ID).active(false).password("hash").build();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(deactivated));

        assertThrows(ConflictException.class, () -> useCase.execute(USER_ID));
        verifyNoInteractions(invitationIssuer);
    }
}
