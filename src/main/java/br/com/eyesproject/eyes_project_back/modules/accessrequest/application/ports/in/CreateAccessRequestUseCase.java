package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;

/**
 * Input port (Use Case) that the Presentation layer will invoke.
 */
public interface CreateAccessRequestUseCase {
    AccessRequest execute(AccessRequest request);
}
