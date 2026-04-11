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

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void execute(String tokenParam, String newPassword) {
        AuthToken resetToken = authTokenRepository.findByTokenAndType(tokenParam, TokenType.RESET)
                .orElseThrow(() -> new DomainException("Link de redefinição inválido ou expirado"));

        if (resetToken.isExpired()) {
            authTokenRepository.deleteById(resetToken.getId());
            throw new DomainException("O link expirou. Solicite uma nova redefinição de senha.");
        }

        User user = resetToken.getUser();
        
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        
        userRepository.save(user);

        // Cleanup the token
        authTokenRepository.deleteById(resetToken.getId());
    }
}
