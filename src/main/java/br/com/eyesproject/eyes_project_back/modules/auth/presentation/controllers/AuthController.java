package br.com.eyesproject.eyes_project_back.modules.auth.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in.LoginUseCase;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.CurrentUserResponse;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
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
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.getToken(), request.getPassword());
        return ResponseEntity.ok().build();
    }
}
