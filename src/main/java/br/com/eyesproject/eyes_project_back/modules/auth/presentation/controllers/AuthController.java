package br.com.eyesproject.eyes_project_back.modules.auth.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in.LoginUseCase;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.CurrentUserResponse;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Sessão, identidade e recuperação de credenciais")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ForgotPasswordUseCase forgotPasswordUseCase;
    private final br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = loginUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CurrentUserResponse.from(user));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar recuperação de senha",
            description = "Sempre responde com sucesso para não revelar se o e-mail possui uma conta ativa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação processada de forma neutra"),
            @ApiResponse(responseCode = "422", description = "E-mail fora do contrato",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Redefinir senha",
            description = "Redefine a senha com um token RESET válido, não expirado e de uso único."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida"),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado, já usado ou de outro tipo",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Token ou senha fora do contrato",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.getToken(), request.getPassword());
        return ResponseEntity.ok().build();
    }
}
