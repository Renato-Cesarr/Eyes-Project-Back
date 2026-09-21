package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import java.util.Locale;

final class AccessRequestTextNormalizer {

    private AccessRequestTextNormalizer() {
    }

    static String email(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    static String name(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
