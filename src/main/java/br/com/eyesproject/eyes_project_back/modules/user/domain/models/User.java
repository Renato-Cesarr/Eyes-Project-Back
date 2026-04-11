package br.com.eyesproject.eyes_project_back.modules.user.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing a User.
 * No JPA or Spring annotations here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private String email;
    private String password;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
