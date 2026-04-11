package br.com.eyesproject.eyes_project_back.modules.user.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthToken {
    private String id;
    private User user;
    private String token;
    private TokenType type;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
