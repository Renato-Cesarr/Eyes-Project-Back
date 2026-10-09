package br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.DetectionEvent;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DetectionRequest(
        @NotNull UUID clientEventId,
        @NotNull DetectionEvent.ObjectClass objectClass,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") @Digits(integer = 1, fraction = 6) BigDecimal confidence,
        @NotNull DetectionEvent.ProximityBand proximityBand,
        @NotNull DetectionEvent.Direction direction,
        @NotNull Instant occurredAt) {
    public DetectionEvent toDomain() {
        return new DetectionEvent(clientEventId, objectClass, confidence, proximityBand, direction, occurredAt);
    }
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown metadata field");
    }
}
