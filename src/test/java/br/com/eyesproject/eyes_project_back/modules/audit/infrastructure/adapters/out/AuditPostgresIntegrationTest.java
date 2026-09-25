package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.support.PostgresContainerIntegrationTest;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditPage;
import br.com.eyesproject.eyes_project_back.modules.audit.application.models.AuditQuery;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.in.LogActionUseCase;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditLogRepository;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditAction;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditLog;
import br.com.eyesproject.eyes_project_back.modules.audit.domain.models.AuditResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class AuditPostgresIntegrationTest extends PostgresContainerIntegrationTest {

    @Autowired LogActionUseCase logActionUseCase;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;

    private String actorUserId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE tb_audit_logs, tb_access_requests, tb_auth_tokens, tb_users CASCADE");
        actorUserId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO tb_users (id, name, email, password, active, role, created_at)
                VALUES (?::uuid, 'Audit Admin', ?, 'encoded', true, 'ADMIN', CURRENT_TIMESTAMP)
                """, actorUserId, "audit-" + actorUserId + "@example.com");
    }

    @Test
    @DisplayName("persists an immutable event and retrieves it through typed filters")
    void persistsAndFiltersEvent() {
        LocalDateTime startedAt = LocalDateTime.now(clock).minusSeconds(1);
        logActionUseCase.execute(AuditLog.builder()
                .action(AuditAction.USER_DEACTIVATED)
                .actorUserId(actorUserId)
                .targetType("USER")
                .targetId(UUID.randomUUID().toString())
                .result(AuditResult.SUCCESS)
                .correlationId("ren-17-postgres-it")
                .metadata(Map.of("active", "false", "token", "must-not-be-persisted"))
                .build());

        AuditPage result = auditLogRepository.search(new AuditQuery(
                0,
                20,
                actorUserId,
                AuditAction.USER_DEACTIVATED,
                AuditResult.SUCCESS,
                startedAt,
                LocalDateTime.now(clock).plusSeconds(1)
        ));

        assertEquals(1, result.totalElements());
        AuditLog event = result.content().getFirst();
        assertEquals("ren-17-postgres-it", event.getCorrelationId());
        assertEquals(Map.of("active", "false"), event.getMetadata());
        assertFalse(event.getId().isBlank());
        assertEquals(1, count("SELECT COUNT(*) FROM tb_audit_logs WHERE result = 'SUCCESS'"));
    }

    @Test
    @DisplayName("keeps the historical actor identifier after the account is removed")
    void keepsActorSnapshotWhenAccountIsRemoved() {
        logActionUseCase.execute(AuditLog.builder()
                .action(AuditAction.USER_INVITED)
                .actorUserId(actorUserId)
                .targetType("USER")
                .targetId(UUID.randomUUID().toString())
                .result(AuditResult.SUCCESS)
                .correlationId("historical-actor")
                .build());

        jdbcTemplate.update("DELETE FROM tb_users WHERE id = ?::uuid", actorUserId);

        assertEquals(1, count("SELECT COUNT(*) FROM tb_audit_logs WHERE actor_user_id = '"
                + actorUserId + "'::uuid"));
    }

    private int count(String sql) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }
}
