package br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record FinishScanRequest(@NotNull Instant endedAt, @NotNull @Valid ScanMetricsRequest metrics) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown metadata field");
    }
}
