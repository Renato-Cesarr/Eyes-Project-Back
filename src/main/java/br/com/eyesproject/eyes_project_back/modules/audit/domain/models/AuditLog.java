package br.com.eyesproject.eyes_project_back.modules.audit.domain.models;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing an Audit Log.
 * No JPA or Spring annotations here.
 */
public class AuditLog {
    private String id;
    private String action;
    private String userId;
    private LocalDateTime timestamp;
}
