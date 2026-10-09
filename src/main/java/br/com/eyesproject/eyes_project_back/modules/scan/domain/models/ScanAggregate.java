package br.com.eyesproject.eyes_project_back.modules.scan.domain.models;

public record ScanAggregate(long sessions, long completedSessions, long announcedEvents,
                            long processedFrames, long inferenceMillisTotal,
                            long ttsLatencySamples, long ttsLatencyMillisTotal) {}
