package br.com.eyesproject.eyes_project_back.global.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim)
                .map(CorsProperties::validateOrigin)
                .distinct()
                .toList();
    }

    private static String validateOrigin(String candidate) {
        if (candidate.isBlank() || candidate.contains("*")) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS deve conter origens explícitas");
        }
        URI origin = URI.create(candidate);
        boolean supportedScheme = "http".equalsIgnoreCase(origin.getScheme())
                || "https".equalsIgnoreCase(origin.getScheme());
        if (!supportedScheme || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null
                || (origin.getPath() != null && !origin.getPath().isBlank())) {
            throw new IllegalArgumentException("Origem CORS inválida: " + candidate);
        }
        return candidate;
    }
}
