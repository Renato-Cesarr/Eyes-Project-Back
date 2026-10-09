package br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanMetadata;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record StartScanRequest(
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        @NotNull UUID clientSessionId,
        @NotNull UUID installationId,
        @NotNull Instant startedAt,
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9._-]+") String modelId,
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9._-]+") String modelVersion,
        @NotNull @AssertTrue Boolean consentGranted,
        @NotBlank @Pattern(regexp = "metadata-sync-v1") String consentVersion) {
    public ScanMetadata toDomain() {
        return new ScanMetadata(clientSessionId, installationId, startedAt, modelId, modelVersion, consentVersion);
    }
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown metadata field");
    }
}
