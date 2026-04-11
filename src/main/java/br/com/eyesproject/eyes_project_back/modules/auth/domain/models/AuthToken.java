package br.com.eyesproject.eyes_project_back.modules.auth.domain.models;

import lombok.*;

/**
 * Pure domain entity representing an authentication token.
 * No JPA or Spring annotations here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {
    private String token;
    private Long expiresIn;
}
