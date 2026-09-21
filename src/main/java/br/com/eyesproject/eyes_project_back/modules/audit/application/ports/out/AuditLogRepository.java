package br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import java.util.List;

/**
 * Output port defining what the domain expects from the infrastructure.
 */
public interface AuditLogRepository {
    void save(AuditLog log);
    List<AuditLog> findByActorUserId(String userId);
}
