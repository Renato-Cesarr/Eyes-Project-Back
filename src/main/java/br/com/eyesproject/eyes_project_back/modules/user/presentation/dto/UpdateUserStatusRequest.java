package br.com.eyesproject.eyes_project_back.modules.user.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "O status ativo é obrigatório") Boolean active
) {
}
