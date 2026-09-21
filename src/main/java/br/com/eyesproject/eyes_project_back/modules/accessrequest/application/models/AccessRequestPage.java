package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;

import java.util.List;

public record AccessRequestPage(
        List<AccessRequest> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public AccessRequestPage {
        content = List.copyOf(content);
    }
}
