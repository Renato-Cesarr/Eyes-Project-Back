package br.com.eyesproject.eyes_project_back.modules.scan.domain.models;

import java.time.Instant;
import java.util.UUID;

public record ScanMetadata(UUID clientSessionId, UUID installationId, Instant startedAt,
                           String modelId, String modelVersion, String consentVersion) {}
