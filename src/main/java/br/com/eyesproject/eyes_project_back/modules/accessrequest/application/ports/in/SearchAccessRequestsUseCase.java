package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestPage;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestQuery;

public interface SearchAccessRequestsUseCase {
    AccessRequestPage execute(AccessRequestQuery query);
}
