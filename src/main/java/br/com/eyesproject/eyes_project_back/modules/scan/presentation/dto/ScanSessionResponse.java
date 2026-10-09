package br.com.eyesproject.eyes_project_back.modules.scan.presentation.dto;

import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanMetrics;
import br.com.eyesproject.eyes_project_back.modules.scan.domain.models.ScanSession;
import java.time.Instant;
import java.util.UUID;

public record ScanSessionResponse(UUID id, UUID clientSessionId, Instant startedAt, Instant endedAt,
                                  Instant expiresAt, String modelId, String modelVersion, ScanMetrics metrics) {
    public static ScanSessionResponse from(ScanSession session) {
        var metadata = session.metadata();
        return new ScanSessionResponse(session.id(), metadata.clientSessionId(), metadata.startedAt(),
                session.endedAt(), session.expiresAt(), metadata.modelId(), metadata.modelVersion(), session.metrics());
    }
}
