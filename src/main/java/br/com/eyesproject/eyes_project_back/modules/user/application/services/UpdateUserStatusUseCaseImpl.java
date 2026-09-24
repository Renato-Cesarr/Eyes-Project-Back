package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.audit.application.services.AdministrativeAudit;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.UpdateUserStatusUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateUserStatusUseCaseImpl implements UpdateUserStatusUseCase {

    private final UserRepository userRepository;
    private final AdministrativeAudit administrativeAudit;

    @Override
    @Transactional
    public UserView execute(String id, boolean active) {
        AuditAction action = active ? AuditAction.USER_ACTIVATED : AuditAction.USER_DEACTIVATED;
        try {
            UserView result = changeStatus(id, active);
            administrativeAudit.success(
                    action,
                    null,
                    "USER",
                    id,
                    Map.of("active", Boolean.toString(active))
            );
            return result;
        } catch (RuntimeException failure) {
            administrativeAudit.failure(action, null, "USER", id, failure);
            throw failure;
        }
    }

    private UserView changeStatus(String id, boolean active) {
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
