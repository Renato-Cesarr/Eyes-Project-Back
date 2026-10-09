package br.com.eyesproject.eyes_project_back.modules.scan.domain.models;

import java.time.Instant;
import java.util.UUID;

public record ScanSession(UUID id, UUID ownerId, ScanMetadata metadata, Instant consentReceivedAt,
                          Instant expiresAt, Instant endedAt, ScanMetrics metrics) {}
