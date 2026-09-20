package br.com.eyesproject.eyes_project_back.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger and general application configurations should be placed here.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Eyes Project API",
                version = "v1",
                description = "API administrativa e de identidade do Eyes Project"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
