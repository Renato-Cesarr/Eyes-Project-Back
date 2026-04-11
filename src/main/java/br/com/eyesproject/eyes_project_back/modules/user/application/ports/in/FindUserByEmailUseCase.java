package br.com.eyesproject.eyes_project_back.modules.user.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import java.util.Optional;

public interface FindUserByEmailUseCase {
    Optional<User> execute(String email);
}
