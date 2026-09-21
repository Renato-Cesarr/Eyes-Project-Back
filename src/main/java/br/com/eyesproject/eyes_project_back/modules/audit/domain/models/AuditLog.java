package br.com.eyesproject.eyes_project_back.modules.audit.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Pure domain entity representing an Audit Log.
 * No JPA or Spring annotations here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    private String id;
    private AuditAction action;
    private String actorUserId;
    private String targetType;
    private String targetId;
    private LocalDateTime timestamp;
}
