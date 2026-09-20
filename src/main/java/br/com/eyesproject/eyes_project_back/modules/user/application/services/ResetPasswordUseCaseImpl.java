package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ResetPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {

    private static final String INVALID_TOKEN_MESSAGE = "Link de redefinição inválido ou expirado";

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(String tokenParam, String newPassword) {
        AuthToken resetToken = authTokenRepository.findByTokenAndTypeForUpdate(tokenParam, TokenType.RESET)
                .orElseThrow(() -> new DomainException(INVALID_TOKEN_MESSAGE));

        if (resetToken.isExpired()) {
            throw new DomainException(INVALID_TOKEN_MESSAGE);
        }

        User user = resetToken.getUser();
        
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        
        userRepository.save(user);

        // Consumed in the same transaction as the password change.
        authTokenRepository.deleteById(resetToken.getId());
    }
}
