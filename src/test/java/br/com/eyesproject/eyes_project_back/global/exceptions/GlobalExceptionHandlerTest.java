package br.com.eyesproject.eyes_project_back.global.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static br.com.eyesproject.eyes_project_back.global.web.CorrelationIdFilter.REQUEST_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ApiProblemFactory());

    @Test
    void mapsDomainExceptionToSafeBadRequestContract() {
        MockHttpServletRequest request = request("domain-correlation");

        var response = handler.handleDomain(new DomainException("Regra conhecida"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Regra conhecida");
        assertThat(response.getBody().getProperties())
                .containsEntry("code", "BUSINESS_RULE_VIOLATION")
                .containsEntry("correlationId", "domain-correlation");
    }

    @Test
    void hidesUnexpectedExceptionAndLogsCorrelationId(CapturedOutput output) {
        MockHttpServletRequest request = request("failure-correlation");
        request.setMethod("POST");

        var response = handler.handleUnexpected(
                new IllegalStateException("database-password-must-never-leak"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail())
                .isEqualTo("Não foi possível concluir a solicitação. Tente novamente mais tarde.")
                .doesNotContain("database-password");
        assertThat(response.getBody().getProperties())
                .containsEntry("code", "INTERNAL_ERROR")
                .containsEntry("correlationId", "failure-correlation");
        assertThat(output).contains("failure-correlation").contains("database-password-must-never-leak");
    }

    private MockHttpServletRequest request(String correlationId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        return request;
    }
}
