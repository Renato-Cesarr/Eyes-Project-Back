package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationIssuerTest {

    @Mock AuthTokenRepository authTokenRepository;
    @Mock EmailSenderPort emailSenderPort;
    @InjectMocks InvitationIssuer invitationIssuer;

    @Test
    void shouldReplacePreviousSetupTokenAndSendNewInvitation() {
        User user = User.builder().id("bf4b6797-750d-4f5d-8277-11c9099a428c")
                .name("Ana").email("ana@example.com").build();
        LocalDateTime before = LocalDateTime.now();

        invitationIssuer.issue(user);

        verify(authTokenRepository).deleteByUserIdAndType(user.getId(), TokenType.SETUP);
        ArgumentCaptor<AuthToken> tokenCaptor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenRepository).save(tokenCaptor.capture());
        AuthToken token = tokenCaptor.getValue();
        assertSame(user, token.getUser());
        assertEquals(TokenType.SETUP, token.getType());
        assertNotNull(token.getToken());
        assertTrue(token.getExpiresAt().isAfter(before.plusHours(47)));
        verify(emailSenderPort).sendInvitationEmail(user.getEmail(), user.getName(), token.getToken());
    }
}
