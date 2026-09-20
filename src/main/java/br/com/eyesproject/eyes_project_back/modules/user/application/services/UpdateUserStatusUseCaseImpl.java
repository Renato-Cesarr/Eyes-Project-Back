package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.UpdateUserStatusUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUserStatusUseCaseImpl implements UpdateUserStatusUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserView execute(String id, boolean active) {
        User current = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (Boolean.TRUE.equals(current.getActive()) == active) {
            return UserView.from(current);
        }

        if (!active && current.getRole() == UserRole.ADMIN
                && userRepository.countActiveByRoleForUpdate(UserRole.ADMIN) <= 1) {
            throw new ConflictException("Não é possível desativar o último administrador ativo");
        }

        User locked = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (active && locked.getPassword() == null) {
            throw new ConflictException("A conta convidada deve ser ativada pelo link de convite");
        }

        locked.setActive(active);
        return UserView.from(userRepository.save(locked));
    }
}
