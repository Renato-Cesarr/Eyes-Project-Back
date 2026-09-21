package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.ratelimit;

import br.com.eyesproject.eyes_project_back.global.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessRequestRateLimitInterceptorTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("limits POST requests per remote address")
    void limitsPostRequestsPerAddress() {
        var interceptor = new AccessRequestRateLimitInterceptor(2, 60, clock);
        MockHttpServletRequest request = request("POST", "10.0.0.1");

        assertDoesNotThrow(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertDoesNotThrow(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        TooManyRequestsException exception = assertThrows(
                TooManyRequestsException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(60, exception.getRetryAfterSeconds());
    }

    @Test
    @DisplayName("does not limit reads and isolates clients")
    void ignoresReadsAndIsolatesClients() {
        var interceptor = new AccessRequestRateLimitInterceptor(1, 60, clock);

        assertTrue(interceptor.preHandle(request("GET", "10.0.0.1"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("POST", "10.0.0.1"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("POST", "10.0.0.2"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    @DisplayName("rejects invalid rate-limit configuration")
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new AccessRequestRateLimitInterceptor(0, 60, clock));
        assertThrows(IllegalArgumentException.class, () -> new AccessRequestRateLimitInterceptor(1, 0, clock));
    }

    private MockHttpServletRequest request(String method, String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/access-requests");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
