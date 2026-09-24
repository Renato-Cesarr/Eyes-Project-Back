package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.audit.application.services.AdministrativeAudit;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
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
    private final AdministrativeAudit administrativeAudit;

    @Override
    @Transactional
    public void execute(String id) {
        try {
            User user = userRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

            if (Boolean.TRUE.equals(user.getActive()) || user.getPassword() != null) {
                throw new ConflictException("Somente convites pendentes podem ser reenviados");
            }

            invitationIssuer.issue(user);
        } catch (RuntimeException failure) {
            administrativeAudit.failure(AuditAction.INVITATION_RESENT, null, "USER", id, failure);
            throw failure;
        }

        administrativeAudit.success(AuditAction.INVITATION_RESENT, null, "USER", id, null);
    }
}
