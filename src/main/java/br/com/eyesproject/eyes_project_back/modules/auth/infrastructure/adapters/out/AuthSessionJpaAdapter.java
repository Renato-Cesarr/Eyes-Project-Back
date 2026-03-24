package br.com.eyesproject.eyes_project_back.modules.auth.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.AuthSessionRepository;
import br.com.eyesproject.eyes_project_back.modules.auth.domain.models.AuthToken;
import org.springframework.stereotype.Component;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
public class AuthSessionJpaAdapter implements AuthSessionRepository {
    @Override
    public void save(AuthToken token) {
        // Implementation logic
    }
}
