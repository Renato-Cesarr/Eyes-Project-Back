package br.com.eyesproject.eyes_project_back.modules.scan.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ScanSessionRepository {
    void lockActiveOwner(UUID ownerId);
    Optional<ScanSession> findByClientId(UUID ownerId, UUID clientSessionId);
    long countOwned(UUID ownerId, Instant now);
    void insert(ScanSession session);
    Optional<ScanSession> findOwned(UUID ownerId, UUID sessionId, Instant now, boolean lock);
    Optional<DetectionEvent> findEvent(UUID sessionId, UUID clientEventId);
    void insertEvent(UUID sessionId, DetectionEvent event);
    int countEvents(UUID sessionId);
    Optional<Instant> latestEventAt(UUID sessionId);
    void finish(UUID sessionId, Instant endedAt, ScanMetrics metrics);
    void deleteOwned(UUID ownerId);
    int purgeExpired(Instant now);
    ScanAggregate aggregate(Instant now);
}
