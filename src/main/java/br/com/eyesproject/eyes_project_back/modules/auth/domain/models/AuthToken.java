package br.com.eyesproject.eyes_project_back.modules.auth.domain.models;

/**
 * Pure domain entity representing an authentication token.
 * No JPA or Spring annotations here.
 */
public class AuthToken {
    private String token;
    private Long expiresIn;
}
