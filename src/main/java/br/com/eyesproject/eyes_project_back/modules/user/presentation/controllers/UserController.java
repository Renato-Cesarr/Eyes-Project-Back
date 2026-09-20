package br.com.eyesproject.eyes_project_back.modules.user.presentation.controllers;

import br.com.eyesproject.eyes_project_back.global.exceptions.ErrorResponse;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.GetUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ResendInvitationUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SearchUsersUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.UpdateUserStatusUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.CreateUserRequest;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.PageResponse;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.SetupPasswordRequest;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.UpdateUserStatusRequest;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.UserResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final SearchUsersUseCase searchUsersUseCase;
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserStatusUseCase updateUserStatusUseCase;
    private final ResendInvitationUseCase resendInvitationUseCase;

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Lista usuários com paginação e filtros. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários"),
            @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN")
    })
    public PageResponse<UserResponse> searchUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "NAME") UserQuery.SortField sortBy,
            @RequestParam(defaultValue = "ASC") UserQuery.SortDirection direction
    ) {
        var result = searchUsersUseCase.execute(
                new UserQuery(page, size, search, role, active, sortBy, direction)
        );
        return PageResponse.from(result, UserResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuário", description = "Retorna dados seguros de um usuário. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserResponse getUser(@PathVariable String id) {
        return UserResponse.from(getUserUseCase.execute(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Alterar status do usuário",
            description = "Ativa ou desativa uma conta sem exclusão física. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "409", description = "Alteração viola uma regra de segurança",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Status ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserResponse updateStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateUserStatusRequest request
    ) {
        return UserResponse.from(updateUserStatusUseCase.execute(id, request.active()));
    }

    @PostMapping("/{id}/resend-invitation")
    @Operation(summary = "Reenviar convite",
            description = "Invalida o convite anterior e envia um novo token de 48 horas. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Convite reenviado"),
            @ApiResponse(responseCode = "409", description = "A conta não possui convite pendente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> resendInvitation(@PathVariable String id) {
        resendInvitationUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(
            summary = "Convidar estudante",
            description = "Cria uma conta STUDENT inativa e envia um link de ativação de uso único. Requer ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convite criado"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
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
