package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;

/**
 * Input port (Use Case) that the Presentation layer will invoke.
 */
public interface CreateUserUseCase {
    User execute(User user);
}
