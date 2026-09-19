package br.com.eyesproject.eyes_project_back.modules.user.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupPasswordRequest {

    @Schema(description = "Token SETUP opaco recebido no convite", example = "b9d49b88-6e38-4be5-9774-cf24aefcc3c1")
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "Nova senha da conta", example = "senha-segura-123", minLength = 6)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;
}
