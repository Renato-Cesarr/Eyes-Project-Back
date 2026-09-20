package br.com.eyesproject.eyes_project_back.modules.user.presentation.controllers;

import br.com.eyesproject.eyes_project_back.global.exceptions.ErrorResponse;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.CreateUserRequest;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.SetupPasswordRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driving Adapter (Presentation). Exposes REST endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Convites administrativos e ativação de contas")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final SetupPasswordUseCase setupPasswordUseCase;

    @PostMapping
    @Operation(
            summary = "Convidar estudante",
            description = "Cria uma conta STUDENT inativa e envia um link de ativação de uso único. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convite criado"),
            @ApiResponse(responseCode = "400", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN"),
            @ApiResponse(responseCode = "422", description = "Dados do convite inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> createUser(@RequestBody @Valid CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
                
        createUserUseCase.execute(user);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/setup-password")
    @Operation(
            summary = "Ativar conta convidada",
            description = "Endpoint público que define a senha e ativa a conta com um token SETUP válido e de uso único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta ativada"),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado, já usado ou de outro tipo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Token ou senha fora do contrato",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> setupPassword(@RequestBody @Valid SetupPasswordRequest request) {
        setupPasswordUseCase.execute(request.getToken(), request.getPassword());
        return ResponseEntity.ok().build();
    }
}
