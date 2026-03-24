package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Infrastructure Adapter implementing the outbound port.
 * Orchestrates JPA calls and mapping.
 */
@Component
public class AuditLogJpaAdapter implements AuditLogRepository {
    @Override
    public void save(AuditLog log) {
        // Implementation logic
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        return Collections.emptyList();
    }
}
