package br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;

/**
 * Input port (Use Case) that the Presentation layer will invoke.
 */
public interface LogActionUseCase {
    void execute(AuditLog log);
}
