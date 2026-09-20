package br.com.eyesproject.eyes_project_back.modules.user.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T, R> PageResponse<R> from(PageResult<T> page, Function<T, R> mapper) {
        return new PageResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages()
        );
    }
}
