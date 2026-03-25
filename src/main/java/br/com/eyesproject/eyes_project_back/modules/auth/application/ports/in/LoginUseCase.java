package br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginRequest;
import br.com.eyesproject.eyes_project_back.modules.auth.presentation.dto.LoginResponse;

/**
 * Input port (Use Case) that the Presentation layer will invoke.
 */
public interface LoginUseCase {
    LoginResponse execute(LoginRequest request);
}
