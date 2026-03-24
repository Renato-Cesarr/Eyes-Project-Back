package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import java.util.Optional;

/**
 * Output port defining what the domain expects from the infrastructure.
 */
public interface AccessRequestRepository {
    AccessRequest save(AccessRequest request);
    Optional<AccessRequest> findById(String id);
}
