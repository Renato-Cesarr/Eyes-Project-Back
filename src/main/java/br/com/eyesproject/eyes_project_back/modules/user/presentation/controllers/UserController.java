package br.com.eyesproject.eyes_project_back.modules.user.presentation.controllers;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.CreateUserUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.in.SetupPasswordUseCase;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.CreateUserRequest;
import br.com.eyesproject.eyes_project_back.modules.user.presentation.dto.SetupPasswordRequest;
import jakarta.validation.Valid;
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
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final SetupPasswordUseCase setupPasswordUseCase;

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody @Valid CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
                
        createUserUseCase.execute(user);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/setup-password")
    public ResponseEntity<Void> setupPassword(@RequestBody @Valid SetupPasswordRequest request) {
        setupPasswordUseCase.execute(request.getToken(), request.getPassword());
        return ResponseEntity.ok().build();
    }
}
