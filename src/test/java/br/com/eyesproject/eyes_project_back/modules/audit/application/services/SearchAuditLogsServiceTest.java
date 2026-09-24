package br.com.eyesproject.eyes_project_back.modules.audit.application.services;

import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAuditLogsServiceTest {

    @Mock AuditLogRepository repository;

    @Test
    void delegatesTheValidatedQueryToTheRepository() {
        AuditQuery query = new AuditQuery(0, 20, null, null, null, null, null);
        AuditPage expected = new AuditPage(List.of(), 0, 20, 0, 0);
        when(repository.search(query)).thenReturn(expected);

        AuditPage result = new SearchAuditLogsService(repository).execute(query);

        assertSame(expected, result);
        verify(repository).search(query);
    }
}
