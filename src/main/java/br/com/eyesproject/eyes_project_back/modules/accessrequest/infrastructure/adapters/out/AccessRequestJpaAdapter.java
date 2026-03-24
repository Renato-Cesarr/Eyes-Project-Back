package br.com.eyesproject.eyes_project_back.modules.accessrequest.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
public class AccessRequestJpaAdapter implements AccessRequestRepository {
    @Override
    public AccessRequest save(AccessRequest request) {
        return request;
    }

    @Override
    public Optional<AccessRequest> findById(String id) {
        return Optional.empty();
    }
}
