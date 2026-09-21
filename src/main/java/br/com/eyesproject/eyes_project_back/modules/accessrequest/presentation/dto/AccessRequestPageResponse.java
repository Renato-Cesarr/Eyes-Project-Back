package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestPage;

import java.util.List;

public record AccessRequestPageResponse(
        List<AccessRequestResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AccessRequestPageResponse from(AccessRequestPage page) {
        return new AccessRequestPageResponse(
                page.content().stream().map(AccessRequestResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
