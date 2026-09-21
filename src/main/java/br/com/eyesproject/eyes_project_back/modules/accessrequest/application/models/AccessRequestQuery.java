package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;

public record AccessRequestQuery(
        int page,
        int size,
        String search,
        AccessRequestStatus status
) {
    private static final int MAX_PAGE_SIZE = 100;

    public AccessRequestQuery {
        if (page < 0) {
            throw new DomainException("A página não pode ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainException("O tamanho da página deve estar entre 1 e 100");
        }
        search = search == null || search.isBlank() ? null : search.trim();
    }
}
