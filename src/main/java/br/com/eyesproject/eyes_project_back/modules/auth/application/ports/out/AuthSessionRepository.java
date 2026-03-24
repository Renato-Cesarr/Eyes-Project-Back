package br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.auth.domain.models.AuthToken;

/**
 * Output port defining what the domain expects from the infrastructure.
 */
public interface AuthSessionRepository {
    void save(AuthToken token);
}
