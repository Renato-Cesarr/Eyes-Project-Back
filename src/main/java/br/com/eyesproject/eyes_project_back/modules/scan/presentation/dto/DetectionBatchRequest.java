package br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record DetectionBatchRequest(@NotNull @Size(min = 1, max = 50) List<@NotNull @Valid DetectionRequest> events) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown metadata field");
    }
}
