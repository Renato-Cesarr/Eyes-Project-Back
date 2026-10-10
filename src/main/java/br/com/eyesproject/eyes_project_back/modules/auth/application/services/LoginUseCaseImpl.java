package br.com.eyesproject.eyes_project_back.modules.auth.application.services;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in.LoginUseCase;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
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
                .orElseThrow(() -> new BadCredentialsException("Dados de acesso inválidos"));

        if (!Boolean.TRUE.equals(user.getActive()) || user.getPassword() == null) {
            throw new BadCredentialsException("Dados de acesso inválidos");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Dados de acesso inválidos");
        }

        String token = tokenProvider.generateToken(user);

        LoginResponse.UserResponse userResponse = LoginResponse.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return LoginResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
}
