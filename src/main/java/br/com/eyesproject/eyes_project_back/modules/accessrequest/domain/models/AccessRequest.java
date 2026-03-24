package br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models;

/**
 * Pure domain entity representing an Access Request.
 * No JPA or Spring annotations here.
 */
public class AccessRequest {
    private String id;
    private String userId;
    private String status;
}
