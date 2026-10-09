package br.com.eyesproject.eyes_project_back.modules.scan.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.scan.application.ports.out.ScanSessionRepository;
import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ScanSessionJdbcAdapter implements ScanSessionRepository {
    private final JdbcTemplate jdbc;

    @Override
    public void lockActiveOwner(UUID ownerId) {
        var owners = jdbc.query("SELECT active FROM tb_users WHERE id = ? FOR UPDATE",
                (row, index) -> row.getBoolean("active"), ownerId);
        if (owners.isEmpty() || !owners.getFirst()) {
            throw new ConflictException("A conta não está disponível para alteração do histórico.");
        }
    }
    @Override
    public Optional<ScanSession> findByClientId(UUID ownerId, UUID clientSessionId) {
        return jdbc.query("SELECT * FROM tb_scan_sessions WHERE owner_id = ? AND client_session_id = ?",
                this::session, ownerId, clientSessionId).stream().findFirst();
    }
    @Override
    public long countOwned(UUID ownerId, Instant now) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_scan_sessions WHERE owner_id = ? AND expires_at > ?",
                Long.class, ownerId, Timestamp.from(now));
    }
    @Override
    public void insert(ScanSession session) {
        var metadata = session.metadata();
        jdbc.update("""
                INSERT INTO tb_scan_sessions (id, owner_id, client_session_id, installation_id, model_id,
                    model_version, consent_version, consent_received_at, started_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, session.id(), session.ownerId(), metadata.clientSessionId(), metadata.installationId(),
                metadata.modelId(), metadata.modelVersion(), metadata.consentVersion(),
                Timestamp.from(session.consentReceivedAt()), Timestamp.from(metadata.startedAt()),
                Timestamp.from(session.expiresAt()));
    }
    @Override
    public Optional<ScanSession> findOwned(UUID ownerId, UUID sessionId, Instant now, boolean lock) {
        String sql = "SELECT * FROM tb_scan_sessions WHERE owner_id = ? AND id = ? AND expires_at > ?";
        if (lock) { sql += " FOR UPDATE"; }
        return jdbc.query(sql, this::session, ownerId, sessionId, Timestamp.from(now)).stream().findFirst();
    }
    @Override
    public Optional<DetectionEvent> findEvent(UUID sessionId, UUID clientEventId) {
        return jdbc.query("SELECT * FROM tb_detection_results WHERE session_id = ? AND client_event_id = ?",
                (row, index) -> new DetectionEvent(row.getObject("client_event_id", UUID.class),
                        DetectionEvent.ObjectClass.valueOf(row.getString("object_class")), row.getBigDecimal("confidence"),
                        DetectionEvent.ProximityBand.valueOf(row.getString("proximity_band")),
                        DetectionEvent.Direction.valueOf(row.getString("direction")), instant(row, "occurred_at")),
                sessionId, clientEventId).stream().findFirst();
    }
    @Override
    public void insertEvent(UUID sessionId, DetectionEvent event) {
        jdbc.update("""
                INSERT INTO tb_detection_results (id, session_id, client_event_id, object_class, confidence,
                    proximity_band, direction, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), sessionId, event.clientEventId(), event.objectClass().name(),
                event.confidence(), event.proximityBand().name(), event.direction().name(), Timestamp.from(event.occurredAt()));
    }
    @Override
    public int countEvents(UUID sessionId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tb_detection_results WHERE session_id = ?", Integer.class, sessionId);
    }
    @Override
    public Optional<Instant> latestEventAt(UUID sessionId) {
        Timestamp latest = jdbc.queryForObject("SELECT MAX(occurred_at) FROM tb_detection_results WHERE session_id = ?",
                Timestamp.class, sessionId);
        return Optional.ofNullable(latest).map(Timestamp::toInstant);
    }
    @Override
    public void finish(UUID sessionId, Instant endedAt, ScanMetrics metrics) {
        jdbc.update("""
                UPDATE tb_scan_sessions SET ended_at = ?, processed_frames = ?, inference_millis_total = ?,
                    tts_latency_samples = ?, tts_latency_millis_total = ? WHERE id = ?
                """, Timestamp.from(endedAt), metrics.processedFrames(), metrics.inferenceMillisTotal(),
                metrics.ttsLatencySamples(), metrics.ttsLatencyMillisTotal(), sessionId);
    }
    @Override
    public void deleteOwned(UUID ownerId) { jdbc.update("DELETE FROM tb_scan_sessions WHERE owner_id = ?", ownerId); }
    @Override
    public int purgeExpired(Instant now) {
        // Small batches avoid one retention transaction monopolizing the table.
        return jdbc.update("""
                DELETE FROM tb_scan_sessions WHERE id IN
                (SELECT id FROM tb_scan_sessions WHERE expires_at <= ? ORDER BY expires_at LIMIT 1000 FOR UPDATE SKIP LOCKED)
                """, Timestamp.from(now));
    }
    @Override
    public ScanAggregate aggregate(Instant now) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS sessions, COUNT(ended_at) AS completed,
                    COALESCE(SUM(processed_frames), 0) AS frames,
                    COALESCE(SUM(inference_millis_total), 0) AS inference,
                    COALESCE(SUM(tts_latency_samples), 0) AS samples,
                    COALESCE(SUM(tts_latency_millis_total), 0) AS latency,
                    (SELECT COUNT(*) FROM tb_detection_results d JOIN tb_scan_sessions s ON s.id = d.session_id
                        WHERE s.expires_at > ?) AS events
                FROM tb_scan_sessions WHERE expires_at > ?
                """, (row, index) -> new ScanAggregate(row.getLong("sessions"), row.getLong("completed"),
                row.getLong("events"), row.getLong("frames"), row.getLong("inference"),
                row.getLong("samples"), row.getLong("latency")), Timestamp.from(now), Timestamp.from(now));
    }
    private ScanSession session(ResultSet row, int index) throws SQLException {
        Instant end = instant(row, "ended_at");
        var metadata = new ScanMetadata(row.getObject("client_session_id", UUID.class),
                row.getObject("installation_id", UUID.class), instant(row, "started_at"),
                row.getString("model_id"), row.getString("model_version"), row.getString("consent_version"));
        ScanMetrics metrics = end == null ? null : new ScanMetrics(row.getInt("processed_frames"),
                row.getLong("inference_millis_total"), row.getInt("tts_latency_samples"), row.getLong("tts_latency_millis_total"));
        return new ScanSession(row.getObject("id", UUID.class), row.getObject("owner_id", UUID.class), metadata,
                instant(row, "consent_received_at"), instant(row, "expires_at"), end, metrics);
    }
    private static Instant instant(ResultSet row, String column) throws SQLException {
        Timestamp value = row.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
