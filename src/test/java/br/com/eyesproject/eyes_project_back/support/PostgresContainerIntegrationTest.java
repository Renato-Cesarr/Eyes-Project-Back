package br.com.eyesproject.eyes_project_back.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresContainerIntegrationTest {

    @Container
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("eyes_project_test")
            .withUsername("eyes_test")
            .withPassword("eyes_test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }
}
