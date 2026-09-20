package br.com.eyesproject.eyes_project_back.modules.user.application.models;

import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;

public record UserQuery(
        int page,
        int size,
        String search,
        UserRole role,
        Boolean active,
        SortField sortBy,
        SortDirection direction
) {
    private static final int MAX_PAGE_SIZE = 100;

    public UserQuery {
        if (page < 0) {
            throw new DomainException("A página não pode ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainException("O tamanho da página deve estar entre 1 e 100");
        }
        search = search == null || search.isBlank() ? null : search.trim();
        sortBy = sortBy == null ? SortField.NAME : sortBy;
        direction = direction == null ? SortDirection.ASC : direction;
    }

    public enum SortField { NAME, EMAIL, CREATED_AT }
    public enum SortDirection { ASC, DESC }
}
