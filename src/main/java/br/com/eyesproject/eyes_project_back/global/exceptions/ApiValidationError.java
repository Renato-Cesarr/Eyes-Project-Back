package br.com.eyesproject.eyes_project_back.global.exceptions;

public record ApiValidationError(String field, String message) {
}
