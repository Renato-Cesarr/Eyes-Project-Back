package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;

public record AccessRequestSubmission(AccessRequest request, boolean created) {
}
