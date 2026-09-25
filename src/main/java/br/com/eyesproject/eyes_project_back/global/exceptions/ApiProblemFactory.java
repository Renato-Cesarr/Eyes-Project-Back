package br.com.eyesproject.eyes_project_back.global.exceptions;

import br.com.eyesproject.eyes_project_back.global.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

@Component
public class ApiProblemFactory {

    private static final String UNKNOWN_CORRELATION_ID = "unavailable";

    public ProblemDetail create(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:eyes-project:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlationId(request));
        // Temporary compatibility alias for clients that predate the RFC 9457 contract.
        problem.setProperty("message", detail);
        return problem;
    }

    public String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value instanceof String correlationId && !correlationId.isBlank()
                ? correlationId
                : UNKNOWN_CORRELATION_ID;
    }
}
