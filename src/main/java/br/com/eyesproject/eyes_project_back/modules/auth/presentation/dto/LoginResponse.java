package br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserResponse user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private String id;
        private String name;
        private String email;
        private UserRole role;
    }
}
