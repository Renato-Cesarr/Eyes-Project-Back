package br.com.eyesproject.eyes_project_back.global.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Valid @NotNull Policy login,
        @Valid @NotNull Policy accessRequest,
        @Valid @NotNull Policy passwordRecovery
) {

    public List<Policy> policies() {
        return List.of(login, accessRequest, passwordRecovery);
    }

    public record Policy(@Positive long capacity, @Positive long windowSeconds) {
        public Duration window() {
            return Duration.ofSeconds(windowSeconds);
        }
    }
}
