package br.com.eyesproject.eyes_project_back.modules.scan.infrastructure;

import br.com.eyesproject.eyes_project_back.modules.scan.application.services.ScanSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scan-collection.cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class ScanRetentionJob {
    private final ScanSessionService service;
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void removeExpiredMetadata() { service.purgeExpired(); }
}
