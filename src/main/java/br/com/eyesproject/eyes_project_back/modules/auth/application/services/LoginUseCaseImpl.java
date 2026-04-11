package br.com.eyesproject.eyes_project_back.modules.auth.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in.LoginUseCase;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Override
    public LoginResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new DomainException("Dados de acesso inválidos"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new DomainException("Conta de usuário desativada");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new DomainException("Dados de acesso inválidos");
        }

        String token = tokenProvider.generateToken(user);

        LoginResponse.UserResponse userResponse = LoginResponse.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return LoginResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
}
