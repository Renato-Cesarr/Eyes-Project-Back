package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;

import java.time.LocalDateTime;

public record AccessRequestResponse(
        String id,
        String name,
        String email,
        String reason,
        AccessRequestStatus status,
        String decisionReason,
        String decidedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime decidedAt
) {
    public static AccessRequestResponse from(AccessRequest request) {
        return new AccessRequestResponse(
                request.getId(),
                request.getName(),
                request.getEmail(),
                request.getRequestReason(),
                request.getStatus(),
                request.getDecisionReason(),
                request.getDecidedByUserId(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getDecidedAt()
        );
    }
}
