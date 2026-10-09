package br.com.eyesproject.eyes_project_back.modules.scan.domain.models;

public record ScanMetrics(int processedFrames, long inferenceMillisTotal,
                          int ttsLatencySamples, long ttsLatencyMillisTotal) {}
