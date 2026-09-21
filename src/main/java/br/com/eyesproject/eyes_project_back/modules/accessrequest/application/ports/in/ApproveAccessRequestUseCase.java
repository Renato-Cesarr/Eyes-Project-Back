package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;

public interface ApproveAccessRequestUseCase {
    AccessRequest execute(String requestId, String administratorId);
}
