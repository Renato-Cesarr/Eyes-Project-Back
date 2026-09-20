package br.com.eyesproject.eyes_project_back.modules.user.application.models;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
