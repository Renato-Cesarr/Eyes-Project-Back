package br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;

/**
 * Output port defining what the domain expects from the infrastructure.
 */
public interface AuditLogRepository {
    void save(AuditLog log);

    AuditPage search(AuditQuery query);
}
