package br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out;

import java.util.Optional;

public interface AuditContextProvider {
    Optional<String> currentActorUserId();

    Optional<String> currentCorrelationId();
}
