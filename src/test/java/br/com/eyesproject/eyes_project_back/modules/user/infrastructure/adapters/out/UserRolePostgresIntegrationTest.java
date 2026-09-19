package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_IT", matches = "true")
@Transactional
class UserRolePostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void migrationPersistsEnumAndDefaultsExistingContractToStudent() {
        User administrator = User.builder()
                .name("Database Administrator")
                .email("database-admin@example.com")
                .password("encoded-password")
                .active(true)
                .role(UserRole.ADMIN)
                .build();

        User saved = userRepository.save(administrator);
        entityManager.flush();

        String persistedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM tb_users WHERE id = ?",
                String.class,
                UUID.fromString(saved.getId())
        );
        assertEquals("ADMIN", persistedRole);

        UUID studentId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO tb_users (id, name, email, password, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                studentId,
                "Default Student",
                "default-student@example.com",
                "encoded-password",
                true,
                LocalDateTime.now()
        );

        String defaultRole = jdbcTemplate.queryForObject(
                "SELECT role FROM tb_users WHERE id = ?",
                String.class,
                studentId
        );
        assertEquals("STUDENT", defaultRole);
    }
}
