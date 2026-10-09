package br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanMetrics;
import jakarta.validation.constraints.*;

public record ScanMetricsRequest(
        @NotNull @Min(0) @Max(1000000) Integer processedFrames,
        @NotNull @Min(0) @Max(1000000000) Long inferenceMillisTotal,
        @NotNull @Min(0) @Max(200) Integer ttsLatencySamples,
        @NotNull @Min(0) @Max(1000000000) Long ttsLatencyMillisTotal) {
    public ScanMetrics toDomain() {
        return new ScanMetrics(processedFrames, inferenceMillisTotal, ttsLatencySamples, ttsLatencyMillisTotal);
    }
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown metadata field");
    }
}
