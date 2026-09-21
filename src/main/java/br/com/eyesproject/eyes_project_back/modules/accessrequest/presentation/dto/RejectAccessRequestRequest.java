package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAccessRequestRequest(
        @NotBlank(message = "Justificativa é obrigatória")
        @Size(max = 500, message = "Justificativa deve ter no máximo 500 caracteres")
        String reason
) {
}
