package br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;

public interface TokenProvider {
    String generateToken(User user);
    String validateToken(String token);
}
