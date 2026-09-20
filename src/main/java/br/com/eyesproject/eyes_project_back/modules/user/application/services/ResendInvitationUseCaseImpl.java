package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ResendInvitationUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendInvitationUseCaseImpl implements ResendInvitationUseCase {

    private final UserRepository userRepository;
    private final InvitationIssuer invitationIssuer;

    @Override
    @Transactional
    public void execute(String id) {
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (Boolean.TRUE.equals(user.getActive()) || user.getPassword() != null) {
            throw new ConflictException("Somente convites pendentes podem ser reenviados");
        }

        invitationIssuer.issue(user);
    }
}
