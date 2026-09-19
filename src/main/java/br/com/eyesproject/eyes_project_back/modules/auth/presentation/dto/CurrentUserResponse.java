package br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;

public record CurrentUserResponse(String id, String name, String email, UserRole role) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
