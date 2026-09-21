package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.ApproveAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.RejectAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.LogActionUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DecideAccessRequestService implements ApproveAccessRequestUseCase, RejectAccessRequestUseCase {

    private final AccessRequestRepository accessRequestRepository;
    private final CreateUserUseCase createUserUseCase;
    private final LogActionUseCase logActionUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public AccessRequest execute(String requestId, String administratorId) {
        AccessRequest request = findForDecision(requestId);
        if (request.getStatus() == AccessRequestStatus.APPROVED) {
            return request;
        }
        ensurePending(request);

        createUserUseCase.execute(User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build());

        request.approve(administratorId, LocalDateTime.now(clock));
        AccessRequest saved = accessRequestRepository.save(request);
        logDecision(AuditAction.ACCESS_REQUEST_APPROVED, administratorId, saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public AccessRequest execute(String requestId, String administratorId, String reason) {
        AccessRequest request = findForDecision(requestId);
        if (request.getStatus() == AccessRequestStatus.REJECTED) {
            return request;
        }
        ensurePending(request);

        String normalizedReason = AccessRequestTextNormalizer.optionalText(reason);
        if (normalizedReason == null) {
            throw new DomainException("Justificativa é obrigatória");
        }

        request.reject(
                administratorId,
                normalizedReason,
                LocalDateTime.now(clock)
        );
        AccessRequest saved = accessRequestRepository.save(request);
        logDecision(AuditAction.ACCESS_REQUEST_REJECTED, administratorId, saved.getId());
        return saved;
    }

    private AccessRequest findForDecision(String requestId) {
        return accessRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de acesso não encontrada"));
    }

    private void ensurePending(AccessRequest request) {
        if (request.getStatus() != AccessRequestStatus.PENDING) {
            throw new ConflictException("A solicitação de acesso já possui uma decisão definitiva");
        }
    }

    private void logDecision(AuditAction action, String administratorId, String requestId) {
        logActionUseCase.execute(AuditLog.builder()
                .action(action)
                .actorUserId(administratorId)
                .targetType("ACCESS_REQUEST")
                .targetId(requestId)
                .timestamp(LocalDateTime.now(clock))
                .build());
    }
}
