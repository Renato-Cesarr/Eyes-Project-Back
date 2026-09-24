package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.SearchAuditLogsUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchAuditLogsService implements SearchAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public AuditPage execute(AuditQuery query) {
        return auditLogRepository.search(query);
    }
}
