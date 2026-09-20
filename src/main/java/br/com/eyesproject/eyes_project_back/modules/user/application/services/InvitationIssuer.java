package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class InvitationIssuer {

    static final long INVITATION_EXPIRATION_HOURS = 48;

    private final AuthTokenRepository authTokenRepository;
    private final EmailSenderPort emailSenderPort;

    void issue(User user) {
        authTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.SETUP);

        String rawToken = UUID.randomUUID().toString();
        AuthToken setupToken = AuthToken.builder()
                .user(user)
                .token(rawToken)
                .type(TokenType.SETUP)
                .expiresAt(LocalDateTime.now().plusHours(INVITATION_EXPIRATION_HOURS))
                .build();

        authTokenRepository.save(setupToken);
        emailSenderPort.sendInvitationEmail(user.getEmail(), user.getName(), rawToken);
    }
}
