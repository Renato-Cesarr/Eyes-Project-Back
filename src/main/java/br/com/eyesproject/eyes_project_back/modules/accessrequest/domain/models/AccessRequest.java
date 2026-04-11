package br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models;

import lombok.*;

/**
 * Pure domain entity representing an Access Request.
 * No JPA or Spring annotations here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessRequest {
    private String id;
    private String userId;
    private String status;
}
