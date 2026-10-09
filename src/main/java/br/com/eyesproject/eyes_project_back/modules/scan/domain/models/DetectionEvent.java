package br.com.eyesproject.eyes_project_back.modules.scan.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DetectionEvent(UUID clientEventId, ObjectClass objectClass, BigDecimal confidence,
                             ProximityBand proximityBand, Direction direction, Instant occurredAt) {
    public enum ObjectClass {
        @com.fasterxml.jackson.annotation.JsonProperty("person") PERSON,
        @com.fasterxml.jackson.annotation.JsonProperty("chair") CHAIR,
        @com.fasterxml.jackson.annotation.JsonProperty("table_desk") TABLE_DESK,
        @com.fasterxml.jackson.annotation.JsonProperty("backpack") BACKPACK
    }
    public enum ProximityBand {
        @com.fasterxml.jackson.annotation.JsonProperty("distant") DISTANT,
        @com.fasterxml.jackson.annotation.JsonProperty("attention") ATTENTION,
        @com.fasterxml.jackson.annotation.JsonProperty("veryNear") VERY_NEAR
    }
    public enum Direction {
        @com.fasterxml.jackson.annotation.JsonProperty("left") LEFT,
        @com.fasterxml.jackson.annotation.JsonProperty("ahead") AHEAD,
        @com.fasterxml.jackson.annotation.JsonProperty("right") RIGHT
    }
}
