package br.com.eyesproject.eyes_project_back.modules.user.domain.models;

public enum UserRole {
    ADMIN,
    STUDENT;

    public String authority() {
        return "ROLE_" + name();
    }
}
