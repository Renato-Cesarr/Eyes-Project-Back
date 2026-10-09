package br.com.eyesproject.eyes_project_back.modules.scan.presentation;

import br.com.eyesproject.eyes_project_back.support.PostgresContainerIntegrationTest;
import br.com.eyesproject.eyes_project_back.modules.scan.application.services.ScanSessionService;
import br.com.eyesproject.eyes_project_back.modules.scan.application.ports.out.ScanSessionRepository;
import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanMetadata;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.scan-collection.enabled=true")
class ScanHttpPostgresIntegrationTest extends PostgresContainerIntegrationTest {
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired ScanSessionRepository repository;
    @Autowired ScanSessionService service;
    private User student;
    private String token;
    private Instant started;

    @BeforeEach
    void setup() throws Exception {
        jdbc.execute("TRUNCATE TABLE tb_users CASCADE");
        student = users.save(User.builder().name("HTTP Student").email("http-student@example.com")
                .password(encoder.encode("Test-only-pass-18!" )).role(UserRole.STUDENT).active(true).build());
        var login = request("POST", "/api/v1/auth/login", """
                {"email":"http-student@example.com","password":"Test-only-pass-18!"}
                """, null);
        assertEquals(200, login.statusCode());
        token = mapper.readTree(login.body()).get("token").asText();
        started = Instant.now().minusSeconds(10);
    }

    @Test
    void realHttpLoginLifecycleRetryAndDeletionUseFlywayPostgresAndSecurity() throws Exception {
        String start = startBody();
        var first = request("POST", "/api/v1/scan-sessions", start, token);
        assertEquals(200, first.statusCode());
        String id = mapper.readTree(first.body()).get("id").asText();
        var retry = request("POST", "/api/v1/scan-sessions", start, token);
        assertEquals(id, mapper.readTree(retry.body()).get("id").asText());
        String event = """
                {"events":[{"clientEventId":"%s","objectClass":"person","confidence":0.8,
                "proximityBand":"attention","direction":"left","occurredAt":"%s"}]}
                """.formatted(UUID.randomUUID(), started.plusSeconds(1));
        String path = "/api/v1/scan-sessions/" + id;
        assertEquals(200, request("POST", path + "/events", event, token).statusCode());
        assertEquals(200, request("POST", path + "/events", event, token).statusCode());
        String finish = """
                {"endedAt":"%s","metrics":{"processedFrames":10,"inferenceMillisTotal":700,
                "ttsLatencySamples":1,"ttsLatencyMillisTotal":300}}
                """.formatted(started.plusSeconds(2));
        assertEquals(200, request("POST", path + "/finish", finish, token).statusCode());
        assertEquals(200, request("GET", path, null, token).statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tb_detection_results", Integer.class));
        assertEquals(204, request("DELETE", "/api/v1/scan-sessions", null, token).statusCode());
        assertEquals(404, request("GET", path, null, token).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tb_detection_results", Integer.class));
    }

    @Test
    void realHttpRejectsChunkedOversizeAndMediaAndMalformedIdentifiersSafely() throws Exception {
        String valid = startBody().trim();
        var media = request("POST", "/api/v1/scan-sessions", valid.substring(0, valid.length() - 1) + ",\"image\":\"blocked\"}", token);
        assertEquals(400, media.statusCode());
        try (var client = HttpClient.newHttpClient()) {
            // ofInputStream has unknown length: the real HTTP request uses chunked transfer.
            var oversized = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/scan-sessions"))
                    .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(new byte[65537]))).build();
            var response = client.send(oversized, HttpResponse.BodyHandlers.ofString());
            assertEquals(413, response.statusCode());
            assertEquals("METADATA_TOO_LARGE", mapper.readTree(response.body()).get("code").asText());
        }
        assertEquals(415, request("POST", "/api/v1/scan-sessions", "not json", token, "text/plain").statusCode());
        assertEquals(400, request("GET", "/api/v1/scan-sessions/not-a-uuid", null, token).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tb_scan_sessions", Integer.class));
    }

    @Test
    void disabledCollectionRejectsAllWritesButKeepsReadAndDeleteAvailable() {
        var disabled = new ScanSessionService(repository, false);
        UUID owner = UUID.fromString(student.getId());
        UUID id = service.start(owner, new ScanMetadata(UUID.randomUUID(), UUID.randomUUID(), started,
                "efficientdet-lite0-coco2017-int8", "tensorflow-metadata-1", "metadata-sync-v1")).id();
        assertThrows(br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException.class,
                () -> disabled.start(owner, new ScanMetadata(UUID.randomUUID(), UUID.randomUUID(), started,
                        "model", "v1", "metadata-sync-v1")));
        assertThrows(br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException.class,
                () -> disabled.append(owner, id, java.util.List.of()));
        assertThrows(br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException.class,
                () -> disabled.finish(owner, id, started.plusSeconds(1), new br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanMetrics(0, 0, 0, 0)));
        assertEquals(id, disabled.get(owner, id).id());
        disabled.deleteHistory(owner);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tb_scan_sessions", Integer.class));
    }

    private String startBody() {
        return """
                {"schemaVersion":1,"clientSessionId":"%s","installationId":"%s","startedAt":"%s",
                "modelId":"efficientdet-lite0-coco2017-int8","modelVersion":"tensorflow-metadata-1",
                "consentGranted":true,"consentVersion":"metadata-sync-v1"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), started);
    }
    private HttpResponse<String> request(String method, String path, String body, String bearer) throws Exception {
        return request(method, path, body, bearer, "application/json");
    }
    private HttpResponse<String> request(String method, String path, String body, String bearer, String contentType) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(10));
            if (bearer != null) { builder.header("Authorization", "Bearer " + bearer); }
            if (body != null) { builder.header("Content-Type", contentType); }
            return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
