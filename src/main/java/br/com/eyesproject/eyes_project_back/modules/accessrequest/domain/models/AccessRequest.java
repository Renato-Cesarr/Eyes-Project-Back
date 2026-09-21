package br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing an Access Request.
 * No JPA or Spring annotations here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRequest {
    private String id;
    private String name;
    private String email;
    private String requestReason;
    @Builder.Default
    private AccessRequestStatus status = AccessRequestStatus.PENDING;
    private String decisionReason;
    private String decidedByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime decidedAt;

    public void approve(String administratorId, LocalDateTime decisionTime) {
        status = AccessRequestStatus.APPROVED;
        decidedByUserId = administratorId;
        decidedAt = decisionTime;
        decisionReason = null;
    }

    public void reject(String administratorId, String reason, LocalDateTime decisionTime) {
        status = AccessRequestStatus.REJECTED;
        decidedByUserId = administratorId;
        decidedAt = decisionTime;
        decisionReason = reason;
    }
}
