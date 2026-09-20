package br.com.eyesproject.eyes_project_back.modules.user.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserView;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String name,
        String email,
        UserRole role,
        boolean active,
        boolean invitationPending,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(UserView view) {
        return new UserResponse(
                view.id(), view.name(), view.email(), view.role(), view.active(),
                view.invitationPending(), view.createdAt(), view.updatedAt()
        );
    }
}
