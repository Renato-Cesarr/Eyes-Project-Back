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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetupPasswordUseCaseImpl implements SetupPasswordUseCase {

    private static final String INVALID_TOKEN_MESSAGE = "Link de ativação inválido ou expirado";

    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(String tokenParam, String newPassword) {
        AuthToken setupToken = authTokenRepository.findByTokenAndTypeForUpdate(tokenParam, TokenType.SETUP)
                .orElseThrow(() -> new DomainException(INVALID_TOKEN_MESSAGE));

        if (setupToken.isExpired()) {
            throw new DomainException(INVALID_TOKEN_MESSAGE);
        }

        User user = setupToken.getUser();
        
        // Setup new encoded password and activate account
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setActive(true);
        
        userRepository.save(user);

        // Consumed in the same transaction as the account activation.
        authTokenRepository.deleteById(setupToken.getId());
    }
}
