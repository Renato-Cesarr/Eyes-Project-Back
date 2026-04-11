package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.AuthTokenRepository;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.AuthToken;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SetupPasswordUseCaseImpl implements SetupPasswordUseCase {

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void execute(String tokenParam, String newPassword) {
        AuthToken setupToken = authTokenRepository.findByTokenAndType(tokenParam, TokenType.SETUP)
                .orElseThrow(() -> new DomainException("Link de ativação inválido ou expirado"));

        if (setupToken.isExpired()) {
            authTokenRepository.deleteById(setupToken.getId());
            throw new DomainException("O link de convite expirou. Solicite um novo acesso.");
        }

        User user = setupToken.getUser();
        
        // Setup new encoded password and activate account
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setActive(true);
        
        userRepository.save(user);

        // Cleanup the token
        authTokenRepository.deleteById(setupToken.getId());
    }
}
