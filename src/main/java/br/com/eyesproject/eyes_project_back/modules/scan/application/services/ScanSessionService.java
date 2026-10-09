package br.com.eyesproject.eyes_project_back.modules.scan.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.global.exceptions.DomainException;
import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.scan.application.ports.out.ScanSessionRepository;
import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ScanSessionService {
    private static final Duration RETENTION = Duration.ofDays(30);
    private static final Duration MAX_DURATION = Duration.ofHours(2);
    private final ScanSessionRepository repository;
    private final boolean collectionEnabled;

    public ScanSessionService(ScanSessionRepository repository,
                              @Value("${app.scan-collection.enabled:false}") boolean collectionEnabled) {
        this.repository = repository;
        this.collectionEnabled = collectionEnabled;
    }

    @Transactional
    public ScanSession start(UUID ownerId, ScanMetadata input) {
        requireCollection();
        Instant now = Instant.now();
        ScanMetadata metadata = new ScanMetadata(input.clientSessionId(), input.installationId(),
                millis(input.startedAt()), input.modelId(), input.modelVersion(), input.consentVersion());
        validateTime(metadata.startedAt(), now);
        repository.lockActiveOwner(ownerId);
        var existing = repository.findByClientId(ownerId, metadata.clientSessionId());
        if (existing.isPresent()) {
            if (!existing.get().metadata().equals(metadata)) {
                throw new ConflictException("A chave da sessão já foi utilizada com outros metadados.");
            }
            return existing.get();
        }
        if (repository.countOwned(ownerId, now) >= 1000) {
            throw new ConflictException("O limite de sessões retidas foi atingido. Exclua o histórico antes de enviar novas sessões.");
        }
        var session = new ScanSession(UUID.randomUUID(), ownerId, metadata, millis(now),
                metadata.startedAt().plus(RETENTION), null, null);
        repository.insert(session);
        return session;
    }

    @Transactional
    public int append(UUID ownerId, UUID sessionId, List<DetectionEvent> events) {
        requireCollection();
        var session = owned(ownerId, sessionId, true);
        int count = repository.countEvents(sessionId);
        for (var input : events) {
            var event = new DetectionEvent(input.clientEventId(), input.objectClass(), input.confidence().stripTrailingZeros(),
                    input.proximityBand(), input.direction(), millis(input.occurredAt()));
            var existing = repository.findEvent(sessionId, event.clientEventId());
            if (existing.isPresent()) {
                if (!sameEvent(existing.get(), event)) {
                    throw new ConflictException("A chave do evento já foi utilizada com outros metadados.");
                }
                continue;
            }
            if (session.endedAt() != null) {
                throw new ConflictException("A sessão finalizada não aceita novos eventos.");
            }
            if (event.occurredAt().isBefore(session.metadata().startedAt())
                    || event.occurredAt().isAfter(session.metadata().startedAt().plus(MAX_DURATION))
                    || event.occurredAt().isAfter(Instant.now().plusSeconds(300))) {
                throw new DomainException("O evento deve ocorrer no intervalo da sessão.");
            }
            if (++count > 200) {
                throw new ConflictException("A sessão excede o limite de 200 eventos anunciados.");
            }
            repository.insertEvent(sessionId, event);
        }
        return count;
    }

    @Transactional
    public ScanSession finish(UUID ownerId, UUID sessionId, Instant inputEnd, ScanMetrics metrics) {
        requireCollection();
        var session = owned(ownerId, sessionId, true);
        Instant endedAt = millis(inputEnd);
        if (session.endedAt() != null) {
            if (!session.endedAt().equals(endedAt) || !session.metrics().equals(metrics)) {
                throw new ConflictException("A sessão já foi finalizada com outros metadados.");
            }
            return session;
        }
        if (endedAt.isBefore(session.metadata().startedAt())
                || endedAt.isAfter(session.metadata().startedAt().plus(MAX_DURATION))
                || endedAt.isAfter(Instant.now().plusSeconds(300))
                || repository.latestEventAt(sessionId).filter(time -> time.isAfter(endedAt)).isPresent()) {
            throw new DomainException("O fim deve incluir todos os eventos e respeitar o intervalo da sessão.");
        }
        int events = repository.countEvents(sessionId);
        if (metrics.ttsLatencySamples() > events || metrics.processedFrames() < events
                || (metrics.ttsLatencySamples() == 0 && metrics.ttsLatencyMillisTotal() != 0)
                || (metrics.processedFrames() == 0 && metrics.inferenceMillisTotal() != 0)) {
            throw new DomainException("As métricas agregadas não correspondem aos eventos da sessão.");
        }
        repository.finish(sessionId, endedAt, metrics);
        return owned(ownerId, sessionId, false);
    }

    @Transactional(readOnly = true)
    public ScanSession get(UUID ownerId, UUID sessionId) { return owned(ownerId, sessionId, false); }

    @Transactional
    public void deleteHistory(UUID ownerId) {
        repository.lockActiveOwner(ownerId);
        repository.deleteOwned(ownerId);
    }

    @Transactional
    public int purgeExpired() { return repository.purgeExpired(Instant.now()); }

    @Transactional(readOnly = true)
    public ScanAggregate aggregate() { return repository.aggregate(Instant.now()); }

    private ScanSession owned(UUID ownerId, UUID sessionId, boolean lock) {
        return repository.findOwned(ownerId, sessionId, Instant.now(), lock)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada."));
    }
    private void requireCollection() {
        if (!collectionEnabled) { throw new ConflictException("A coleta remota de metadados está desabilitada."); }
    }
    private static void validateTime(Instant time, Instant now) {
        if (!time.isAfter(now.minus(RETENTION)) || time.isAfter(now.plusSeconds(300))) {
            throw new DomainException("O início deve estar nos últimos 30 dias, com tolerância de relógio de cinco minutos.");
        }
    }
    private static Instant millis(Instant time) { return time.truncatedTo(ChronoUnit.MILLIS); }
    private static boolean sameEvent(DetectionEvent left, DetectionEvent right) {
        return left.clientEventId().equals(right.clientEventId()) && left.objectClass() == right.objectClass()
                && left.confidence().compareTo(right.confidence()) == 0 && left.proximityBand() == right.proximityBand()
                && left.direction() == right.direction() && left.occurredAt().equals(right.occurredAt());
    }
}
