package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestPage;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestQuery;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import java.util.Optional;

/**
 * Output port defining what the domain expects from the infrastructure.
 */
public interface AccessRequestRepository {
    AccessRequest save(AccessRequest request);
    Optional<AccessRequest> findById(String id);
    Optional<AccessRequest> findByIdForUpdate(String id);
    Optional<AccessRequest> findPendingByEmail(String normalizedEmail);
    AccessRequestPage search(AccessRequestQuery query);
}
