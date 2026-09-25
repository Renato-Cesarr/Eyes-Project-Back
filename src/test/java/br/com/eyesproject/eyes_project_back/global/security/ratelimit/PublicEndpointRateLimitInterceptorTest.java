package br.com.eyesproject.eyes_project_back.global.security.ratelimit;

import br.com.eyesproject.eyes_project_back.global.exceptions.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicEndpointRateLimitInterceptorTest {

    private final RateLimitProperties.Policy oneRequest = new RateLimitProperties.Policy(1, 60);
    private final PublicEndpointRateLimitInterceptor interceptor = new PublicEndpointRateLimitInterceptor(
            new RateLimitProperties(oneRequest, oneRequest, oneRequest));

    @Test
    void limitsEverySensitivePublicFlowIndependently() {
        assertLimitedOnSecondRequest("/api/v1/auth/login", "10.0.0.1");
        assertLimitedOnSecondRequest("/api/v1/access-requests", "10.0.0.2");
        assertLimitedOnSecondRequest("/api/v1/auth/forgot-password", "10.0.0.3");
        assertLimitedOnSecondRequest("/api/v1/auth/reset-password", "10.0.0.4");
    }

    @Test
    void keepsDifferentClientsAndUnprotectedRoutesIndependent() {
        assertThat(call("/api/v1/auth/login", "10.0.0.5", "POST")).isTrue();
        assertThat(call("/api/v1/auth/login", "10.0.0.6", "POST")).isTrue();
        assertThat(call("/api/v1/auth/me", "10.0.0.5", "GET")).isTrue();
        assertThat(call("/api/v1/users/setup-password", "10.0.0.5", "POST")).isTrue();
    }

    private void assertLimitedOnSecondRequest(String path, String address) {
        assertThat(call(path, address, "POST")).isTrue();
        assertThatThrownBy(() -> call(path, address, "POST"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(exception -> assertThat(((TooManyRequestsException) exception)
                        .getRetryAfterSeconds()).isPositive());
    }

    private boolean call(String path, String address, String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, new Object());
        if ("POST".equals(method) && !path.endsWith("/me") && !path.endsWith("setup-password")) {
            assertThat(response.getHeader(PublicEndpointRateLimitInterceptor.REMAINING_HEADER)).isNotNull();
        }
        return allowed;
    }
}
