package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record InitialAdminProperties(boolean enabled, String name, String email, String password) {
}
