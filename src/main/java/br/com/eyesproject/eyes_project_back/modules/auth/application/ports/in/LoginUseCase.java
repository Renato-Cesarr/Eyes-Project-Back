package br.com.eyesproject.eyes_project_back.modules.auth.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.auth.domain.models.AuthToken;

/**
 * Input port (Use Case) that the Presentation layer will invoke.
 */
public interface LoginUseCase {
    AuthToken login(String email, String password);
}
