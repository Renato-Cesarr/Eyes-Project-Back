package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestSubmission;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in.SubmitAccessRequestUseCase;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitAccessRequestService implements SubmitAccessRequestUseCase {

    private static final String REGISTERED_EMAIL_MESSAGE =
            "Não é possível criar uma solicitação para este e-mail";

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;

    @Override
    public AccessRequestSubmission execute(AccessRequest input) {
        String normalizedEmail = AccessRequestTextNormalizer.email(input.getEmail());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException(REGISTERED_EMAIL_MESSAGE);
        }

        var existingRequest = accessRequestRepository.findPendingByEmail(normalizedEmail);
        if (existingRequest.isPresent()) {
            return new AccessRequestSubmission(existingRequest.get(), false);
        }

        AccessRequest request = AccessRequest.builder()
                .name(AccessRequestTextNormalizer.name(input.getName()))
                .email(normalizedEmail)
                .requestReason(AccessRequestTextNormalizer.optionalText(input.getRequestReason()))
                .status(AccessRequestStatus.PENDING)
                .build();

        try {
            return new AccessRequestSubmission(accessRequestRepository.save(request), true);
        } catch (ConflictException concurrentConflict) {
            return accessRequestRepository.findPendingByEmail(normalizedEmail)
                    .map(existing -> new AccessRequestSubmission(existing, false))
                    .orElseThrow(() -> concurrentConflict);
        }
    }
}
