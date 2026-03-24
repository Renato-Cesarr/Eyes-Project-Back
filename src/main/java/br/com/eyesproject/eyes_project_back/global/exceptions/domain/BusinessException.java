package br.com.eyesproject.eyes_project_back.global.exceptions.domain;

/**
 * Superclass for every business-related exception within the domain layers.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
