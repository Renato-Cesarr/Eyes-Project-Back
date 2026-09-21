package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final InvitationIssuer invitationIssuer;

    @Override
    @Transactional
    public User execute(User userParam) {
        userParam.setName(userParam.getName().trim().replaceAll("\\s+", " "));
        userParam.setEmail(userParam.getEmail().trim().toLowerCase(Locale.ROOT));

        if (userRepository.findByEmail(userParam.getEmail()).isPresent()) {
            throw new ConflictException("Este e-mail já está cadastrado no sistema");
        }

        // Domain preparation: no password yet, inactive until setup.
        userParam.setActive(false);
        userParam.setPassword(null);
        userParam.setRole(UserRole.STUDENT);

        User savedUser = userRepository.save(userParam);
        invitationIssuer.issue(savedUser);

        return savedUser;
    }
}
