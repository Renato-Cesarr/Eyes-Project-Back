package br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in;

import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;

public interface SearchAuditLogsUseCase {
    AuditPage execute(AuditQuery query);
}
