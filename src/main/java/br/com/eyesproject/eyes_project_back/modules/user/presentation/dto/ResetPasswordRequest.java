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
public class ResetPasswordRequest {

    @Schema(description = "Token RESET opaco recebido por e-mail", example = "0c74f7e9-3828-457c-b8b1-bda8d4f4d733")
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "Nova senha da conta", example = "senha-segura-123", minLength = 6)
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
