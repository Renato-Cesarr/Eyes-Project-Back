package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ForgotPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.EmailSenderPort;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCaseImpl implements ForgotPasswordUseCase {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final EmailSenderPort emailSenderPort;

    @Override
    public void execute(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Se o usuário não existir ou não estiver ativo, nós não dizemos explicitamente 
        // por segurança (evitar enumeration). Apenas saímos silenciosamente ou logamos.
        if (user == null || Boolean.FALSE.equals(user.getActive())) {
            return;
        }

        // Limpa tokens antigos caso o usuário clique várias vezes
        authTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.RESET);

        String rawToken = UUID.randomUUID().toString();

        AuthToken resetToken = AuthToken.builder()
                .user(user)
                .token(rawToken)
                .type(TokenType.RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // 15 mins expiry for reset
                .build();

        authTokenRepository.save(resetToken);

        emailSenderPort.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
    }
}
