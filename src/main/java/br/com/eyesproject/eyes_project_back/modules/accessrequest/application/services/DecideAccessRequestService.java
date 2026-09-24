package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.ApproveAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.RejectAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import br.com.eyesproject.eyes_project_back.modules.audit.application.services.AdministrativeAudit;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DecideAccessRequestService implements ApproveAccessRequestUseCase, RejectAccessRequestUseCase {

    private static final String ACCESS_REQUEST_TARGET = "ACCESS_REQUEST";
    private final AccessRequestRepository accessRequestRepository;
    private final CreateUserUseCase createUserUseCase;
    private final AdministrativeAudit administrativeAudit;
    private final Clock clock;

    @Override
    @Transactional
    public AccessRequest execute(String requestId, String administratorId) {
        try {
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
            administrativeAudit.success(
                    AuditAction.ACCESS_REQUEST_APPROVED,
                    administratorId,
                    ACCESS_REQUEST_TARGET,
                    saved.getId(),
                    Map.of("decision", "APPROVED")
            );
            return saved;
        } catch (RuntimeException failure) {
            administrativeAudit.failure(
                    AuditAction.ACCESS_REQUEST_APPROVED,
                    administratorId,
                    ACCESS_REQUEST_TARGET,
                    requestId,
                    failure
            );
            throw failure;
        }
    }

    @Override
    @Transactional
    public AccessRequest execute(String requestId, String administratorId, String reason) {
        try {
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
            administrativeAudit.success(
                    AuditAction.ACCESS_REQUEST_REJECTED,
                    administratorId,
                    ACCESS_REQUEST_TARGET,
                    saved.getId(),
                    Map.of("decision", "REJECTED", "reasonProvided", "true")
            );
            return saved;
        } catch (RuntimeException failure) {
            administrativeAudit.failure(
                    AuditAction.ACCESS_REQUEST_REJECTED,
                    administratorId,
                    ACCESS_REQUEST_TARGET,
                    requestId,
                    failure
            );
            throw failure;
        }
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

}
