package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
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
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final EmailSenderPort emailSenderPort;

    @Override
    public User execute(User userParam) {
        if (userRepository.findByEmail(userParam.getEmail()).isPresent()) {
            throw new RuntimeException("User email already exists");
        }

        // Domain preparation: no password yet, inactive until setup.
        userParam.setActive(false);
        userParam.setPassword(null);

        User savedUser = userRepository.save(userParam);

        // Generate invitation token
        String rawToken = UUID.randomUUID().toString();
        
        AuthToken setupToken = AuthToken.builder()
                .user(savedUser)
                .token(rawToken)
                .type(TokenType.SETUP)
                .expiresAt(LocalDateTime.now().plusHours(48)) // 48h to expire
                .build();
                
        authTokenRepository.save(setupToken);

        // Send Email
        emailSenderPort.sendInvitationEmail(savedUser.getEmail(), savedUser.getName(), rawToken);

        return savedUser;
    }
}
