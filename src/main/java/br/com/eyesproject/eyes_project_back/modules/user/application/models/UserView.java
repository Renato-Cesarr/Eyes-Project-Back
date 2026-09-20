package br.com.eyesproject.eyes_project_back.modules.user.application.models;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;

import java.time.LocalDateTime;

public record UserView(
        String id,
        String name,
        String email,
        UserRole role,
        boolean active,
        boolean invitationPending,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserView from(User user) {
        return new UserView(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                Boolean.TRUE.equals(user.getActive()),
                !Boolean.TRUE.equals(user.getActive()) && user.getPassword() == null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
