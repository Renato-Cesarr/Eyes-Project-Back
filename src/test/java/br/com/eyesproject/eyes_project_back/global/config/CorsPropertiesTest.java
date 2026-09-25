package br.com.eyesproject.eyes_project_back.global.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesTest {

    @Test
    void normalizesAndDeduplicatesExplicitOrigins() {
        CorsProperties properties = new CorsProperties(List.of(
                " https://app.example.com ",
                "https://app.example.com",
                "http://localhost:4200"
        ));

        assertThat(properties.allowedOrigins())
                .containsExactly("https://app.example.com", "http://localhost:4200");
    }

    @Test
    void rejectsWildcardsAndValuesThatAreNotOrigins() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("https://app.example.com/private")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
