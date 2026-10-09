package br.com.eyesproject.eyes_project_back.modules.scan.presentation;

import br.com.eyesproject.eyes_project_back.support.PostgresContainerIntegrationTest;
import br.com.eyesproject.eyes_project_back.modules.auth.application.ports.out.TokenProvider;
import br.com.eyesproject.eyes_project_back.modules.scan.application.services.ScanSessionService;
import br.com.eyesproject.eyes_project_back.modules.scan.infrastructure.ScanRequestSizeFilter;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.scan-collection.enabled=true")
class ScanSessionPostgresIntegrationTest extends PostgresContainerIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired TokenProvider tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired ScanSessionService service;
    @Autowired ScanRequestSizeFilter sizeFilter;
    private MockMvc mvc;
    private User student;
    private User other;
    private User admin;
    private Instant started;
    private UUID installation;

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE TABLE tb_users CASCADE");
        student = user("Student", UserRole.STUDENT);
        other = user("Other", UserRole.STUDENT);
        admin = user("Admin", UserRole.ADMIN);
        started = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
        installation = UUID.randomUUID();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(sizeFilter).apply(springSecurity()).build();
    }

    @Test
    void lifecycleIsIdempotentAndAdminSeesOnlyTotals() throws Exception {
        UUID client = UUID.randomUUID();
        UUID id = start(student, client);
        assertEquals(id, start(student, client));
        UUID event = UUID.randomUUID();
        String body = batch(event, "person", "0.9", started.plusSeconds(1));
        postJson(student, "/" + id + "/events", body, 200);
        postJson(student, "/" + id + "/events", body, 200);
        String finish = finishBody(started.plusSeconds(5), 20, 1000, 1, 700);
        postJson(student, "/" + id + "/finish", finish, 200);
        postJson(student, "/" + id + "/finish", finish, 200);
        postJson(student, "/" + id + "/events", body, 200);
        assertEquals(1, count("tb_scan_sessions"));
        assertEquals(1, count("tb_detection_results"));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM pg_indexes WHERE indexname IN ('ix_scan_sessions_owner_started', 'ix_scan_sessions_expires', 'ix_detection_results_session_occurred')", Integer.class));
        mvc.perform(get("/api/v1/scan-sessions/{id}", id).header("Authorization", bearer(student)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.metrics.processedFrames").value(20))
                .andExpect(jsonPath("$.ownerId").doesNotExist()).andExpect(jsonPath("$.installationId").doesNotExist());
        mvc.perform(get("/api/v1/scan-sessions/aggregate").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sessions").value(1))
                .andExpect(jsonPath("$.completedSessions").value(1)).andExpect(jsonPath("$.announcedEvents").value(1))
                .andExpect(jsonPath("$.processedFrames").value(20)).andExpect(jsonPath("$.ttsLatencyMillisTotal").value(700))
                .andExpect(jsonPath("$.ownerId").doesNotExist()).andExpect(jsonPath("$.events").doesNotExist());
    }

    @Test
    void authenticationAndOwnershipCoverEveryEndpoint() throws Exception {
        UUID id = start(student, UUID.randomUUID());
        String event = batch(UUID.randomUUID(), "chair", "0.8", started.plusSeconds(1));
        String finish = finishBody(started.plusSeconds(2), 1, 1, 0, 0);
        for (User stranger : new User[]{other, admin}) {
            mvc.perform(get("/api/v1/scan-sessions/{id}", id).header("Authorization", bearer(stranger)))
                    .andExpect(status().isNotFound());
            postJson(stranger, "/" + id + "/events", event, 404);
            postJson(stranger, "/" + id + "/finish", finish, 404);
        }
        mvc.perform(get("/api/v1/scan-sessions/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/scan-sessions").contentType(MediaType.APPLICATION_JSON)
                .content(startBody(UUID.randomUUID()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/scan-sessions/{id}/events", id).contentType(MediaType.APPLICATION_JSON)
                .content(event)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/scan-sessions/{id}/finish", id).contentType(MediaType.APPLICATION_JSON)
                .content(finish)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/scan-sessions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/scan-sessions/aggregate").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE tb_users SET active = false WHERE id = ?", UUID.fromString(student.getId()));
        mvc.perform(get("/api/v1/scan-sessions/{id}", id).header("Authorization", bearer(student)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void strictSchemaRejectsMediaConsentBypassUnknownAndOutOfRangeFields() throws Exception {
        String valid = startBody(UUID.randomUUID());
        postJson(student, "", valid.replace("true", "false"), 422);
        postJson(student, "", valid.replace("metadata-sync-v1", "unknown"), 422);
        postJson(student, "", valid.replace("schemaVersion\":1", "schemaVersion\":2"), 422);
        postJson(student, "", valid.substring(0, valid.length() - 1) + ",\"frame\":\"not-stored\"}", 400);
        postJson(student, "", "{}", 422);
        UUID id = start(student, UUID.randomUUID());
        String event = batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(1));
        postJson(student, "/" + id + "/events", event.replace("\"person\"", "\"unknown\""), 400);
        postJson(student, "/" + id + "/events", event.replace("0.8", "1.1"), 422);
        postJson(student, "/" + id + "/events", event.replace("0.8", "0.1234567"), 422);
        postJson(student, "/" + id + "/events", event.replace("\"direction\"", "\"image\""), 400);
        postJson(student, "/" + id + "/events", "{\"events\":[]}", 422);
        postJson(student, "/" + id + "/events", "{\"events\":[null]}", 422);
        String finish = finishBody(started.plusSeconds(5), 1, 10, 0, 0);
        postJson(student, "/" + id + "/finish", finish.replace("\"processedFrames\"", "\"rawAudio\""), 400);
        assertEquals(0, count("tb_detection_results"));
    }

    @Test
    void conflictingRetriesRollbackTheWholeBatchAndFinishIsImmutable() throws Exception {
        UUID client = UUID.randomUUID();
        UUID id = start(student, client);
        postJson(student, "", startBody(client).replace("model-v1", "model-v2"), 409);
        UUID existing = UUID.randomUUID();
        postJson(student, "/" + id + "/events", batch(existing, "chair", "0.8", started.plusSeconds(1)), 200);
        String fresh = eventBody(UUID.randomUUID(), "person", "0.9", started.plusSeconds(2));
        String conflict = eventBody(existing, "chair", "0.7", started.plusSeconds(1));
        postJson(student, "/" + id + "/events", "{\"events\":[" + fresh + "," + conflict + "]}", 409);
        assertEquals(1, count("tb_detection_results"));
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(5), 10, 100, 1, 50), 200);
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(6), 10, 100, 1, 50), 409);
        postJson(student, "/" + id + "/events", batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(2)), 409);
    }

    @Test
    void timeAndAggregateConsistencyAreValidated() throws Exception {
        postJson(student, "", startBody(UUID.randomUUID()).replace(started.toString(), Instant.now().plusSeconds(600).toString()), 400);
        postJson(student, "", startBody(UUID.randomUUID()).replace(started.toString(), Instant.now().minusSeconds(31 * 86400).toString()), 400);
        UUID id = start(student, UUID.randomUUID());
        postJson(student, "/" + id + "/events", batch(UUID.randomUUID(), "person", "0.8", started.minusSeconds(1)), 400);
        postJson(student, "/" + id + "/events", batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(7201)), 400);
        postJson(student, "/" + id + "/events", batch(UUID.randomUUID(), "person", "0.8", Instant.now().plusSeconds(600)), 400);
        postJson(student, "/" + id + "/events", batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(5)), 200);
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(4), 1, 1, 0, 0), 400);
        postJson(student, "/" + id + "/finish", finishBody(started.minusSeconds(1), 1, 1, 0, 0), 400);
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(7201), 1, 1, 0, 0), 400);
        postJson(student, "/" + id + "/finish", finishBody(Instant.now().plusSeconds(600), 1, 1, 0, 0), 400);
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(6), 0, 1, 0, 0), 400);
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(6), 1, 1, 2, 10), 400);
        postJson(student, "/" + id + "/finish", finishBody(started.plusSeconds(6), 1, 1, 0, 10), 400);
    }

    @Test
    void boundedBatchesAndSessionQuotaPreventUnboundedStorage() throws Exception {
        UUID id = start(student, UUID.randomUUID());
        var eventBodies = new ArrayList<String>();
        for (int index = 0; index < 51; index++) {
            eventBodies.add(eventBody(UUID.randomUUID(), "backpack", "0.8", started.plusSeconds(1)));
        }
        postJson(student, "/" + id + "/events", "{\"events\":[" + String.join(",", eventBodies) + "]}", 422);
        for (int batch = 0; batch < 4; batch++) {
            eventBodies.clear();
            for (int index = 0; index < 50; index++) {
                eventBodies.add(eventBody(UUID.randomUUID(), "table_desk", "0.8", started.plusSeconds(1)));
            }
            postJson(student, "/" + id + "/events", "{\"events\":[" + String.join(",", eventBodies) + "]}", 200);
        }
        postJson(student, "/" + id + "/events", batch(UUID.randomUUID(), "chair", "0.8", started.plusSeconds(1)), 409);
        assertEquals(200, count("tb_detection_results"));
        jdbc.update("""
                INSERT INTO tb_scan_sessions (id, owner_id, client_session_id, installation_id, model_id,
                    model_version, consent_version, consent_received_at, started_at, expires_at)
                SELECT gen_random_uuid(), owner_id, gen_random_uuid(), installation_id, model_id,
                    model_version, consent_version, consent_received_at, started_at, expires_at
                FROM tb_scan_sessions CROSS JOIN generate_series(1, 999) WHERE id = ?
                """, id);
        postJson(student, "", startBody(UUID.randomUUID()), 409);
        assertEquals(id, start(student, jdbc.queryForObject("SELECT client_session_id FROM tb_scan_sessions WHERE id = ?", UUID.class, id)));
    }

    @Test
    void deleteAndRetentionPhysicallyRemoveEventsWithoutAffectingOtherOwners() throws Exception {
        UUID mine = start(student, UUID.randomUUID());
        UUID theirs = start(other, UUID.randomUUID());
        postJson(student, "/" + mine + "/events", batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(1)), 200);
        mvc.perform(delete("/api/v1/scan-sessions").header("Authorization", bearer(student))).andExpect(status().isNoContent());
        assertEquals(0, count("tb_detection_results"));
        assertEquals(1, count("tb_scan_sessions"));
        postJson(other, "/" + theirs + "/events", batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(1)), 200);
        jdbc.update("UPDATE tb_scan_sessions SET started_at = started_at - INTERVAL '31 days', expires_at = expires_at - INTERVAL '31 days' WHERE id = ?", theirs);
        mvc.perform(get("/api/v1/scan-sessions/{id}", theirs).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/scan-sessions/aggregate").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sessions").value(0)).andExpect(jsonPath("$.announcedEvents").value(0));
        assertEquals(1, service.purgeExpired());
        assertEquals(0, service.purgeExpired());
        assertEquals(0, count("tb_detection_results"));
        assertEquals(0, count("tb_scan_sessions"));
    }

    @Test
    void concurrentRetriesCreateOneSessionAndOneEvent() throws Exception {
        UUID client = UUID.randomUUID();
        var startSignal = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { startSignal.await(); return start(student, client); });
            var second = executor.submit(() -> { startSignal.await(); return start(student, client); });
            startSignal.countDown();
            UUID id = first.get();
            assertEquals(id, second.get());
            String batch = batch(UUID.randomUUID(), "person", "0.8", started.plusSeconds(1));
            var firstBatch = executor.submit(() -> { postJson(student, "/" + id + "/events", batch, 200); return true; });
            var secondBatch = executor.submit(() -> { postJson(student, "/" + id + "/events", batch, 200); return true; });
            assertTrue(firstBatch.get());
            assertTrue(secondBatch.get());
        }
        assertEquals(1, count("tb_scan_sessions"));
        assertEquals(1, count("tb_detection_results"));
    }

    @Test
    void oversizedChunkedBodyIsRejectedBeforeDeserialization() throws Exception {
        mvc.perform(post("/api/v1/scan-sessions").header("Authorization", bearer(student))
                .contentType(MediaType.APPLICATION_JSON).content(" ".repeat(65537)))
                .andExpect(status().is(413)).andExpect(jsonPath("$.code").value("METADATA_TOO_LARGE"));
        assertEquals(0, count("tb_scan_sessions"));
    }

    private User user(String name, UserRole role) {
        return users.save(User.builder().name(name).email(name.toLowerCase() + "@example.com")
                .password("unused-test-hash").role(role).active(true).build());
    }
    private String bearer(User user) { return "Bearer " + tokens.generateToken(user); }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private UUID start(User user, UUID client) throws Exception {
        String json = postJson(user, "", startBody(client), 200);
        return UUID.fromString(mapper.readTree(json).get("id").asText());
    }
    private String postJson(User user, String suffix, String body, int code) throws Exception {
        return mvc.perform(post("/api/v1/scan-sessions" + suffix).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(code))
                .andReturn().getResponse().getContentAsString();
    }
    private String startBody(UUID client) {
        return """
                {"schemaVersion":1,"clientSessionId":"%s","installationId":"%s","startedAt":"%s",
                "modelId":"efficientdet-lite0-coco2017-int8","modelVersion":"model-v1",
                "consentGranted":true,"consentVersion":"metadata-sync-v1"}
                """.formatted(client, installation, started).trim();
    }
    private String batch(UUID event, String kind, String confidence, Instant at) {
        return "{\"events\":[" + eventBody(event, kind, confidence, at) + "]}";
    }
    private String eventBody(UUID event, String kind, String confidence, Instant at) {
        return """
                {"clientEventId":"%s","objectClass":"%s","confidence":%s,
                "proximityBand":"veryNear","direction":"ahead","occurredAt":"%s"}
                """.formatted(event, kind, confidence, at).trim();
    }
    private String finishBody(Instant ended, int frames, long inference, int samples, long latency) {
        return """
                {"endedAt":"%s","metrics":{"processedFrames":%d,"inferenceMillisTotal":%d,
                "ttsLatencySamples":%d,"ttsLatencyMillisTotal":%d}}
                """.formatted(ended, frames, inference, samples, latency).trim();
    }
}
