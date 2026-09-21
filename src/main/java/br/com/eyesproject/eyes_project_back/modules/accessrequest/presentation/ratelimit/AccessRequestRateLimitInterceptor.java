package br.com.eyesproject.eyes_project_back.modules.accessrequest.presentation.ratelimit;

import br.com.eyesproject.eyes_project_back.global.exceptions.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AccessRequestRateLimitInterceptor implements HandlerInterceptor {

    private static final int CLEANUP_INTERVAL = 256;

    private final int maximumRequests;
    private final Duration windowDuration;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger requestsSinceCleanup = new AtomicInteger();

    public AccessRequestRateLimitInterceptor(
            @Value("${app.access-requests.rate-limit.max-requests:5}") int maximumRequests,
            @Value("${app.access-requests.rate-limit.window-seconds:900}") long windowSeconds,
            Clock clock
    ) {
        if (maximumRequests < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Configuração de rate limit deve ser positiva");
        }
        this.maximumRequests = maximumRequests;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }

        Instant now = clock.instant();
        AtomicReference<Decision> decision = new AtomicReference<>();
        windows.compute(clientKey(request), (ignored, current) -> nextWindow(current, now, decision));
        cleanupExpiredWindows(now);

        Decision result = decision.get();
        if (!result.allowed()) {
            long retryAfter = Math.max(1, Duration.between(now, result.resetAt()).toSeconds());
            throw new TooManyRequestsException(
                    "Muitas solicitações de acesso. Tente novamente mais tarde.",
                    retryAfter
            );
        }
        return true;
    }

    private Window nextWindow(Window current, Instant now, AtomicReference<Decision> decision) {
        if (current == null || !now.isBefore(current.resetAt())) {
            Instant resetAt = now.plus(windowDuration);
            decision.set(new Decision(true, resetAt));
            return new Window(1, resetAt);
        }
        if (current.requestCount() >= maximumRequests) {
            decision.set(new Decision(false, current.resetAt()));
            return current;
        }
        decision.set(new Decision(true, current.resetAt()));
        return new Window(current.requestCount() + 1, current.resetAt());
    }

    private String clientKey(HttpServletRequest request) {
        // X-Forwarded-For is intentionally ignored until a trusted reverse proxy is configured.
        return request.getRemoteAddr();
    }

    private void cleanupExpiredWindows(Instant now) {
        if (requestsSinceCleanup.incrementAndGet() < CLEANUP_INTERVAL) {
            return;
        }
        requestsSinceCleanup.set(0);
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetAt()));
    }

    private record Window(int requestCount, Instant resetAt) {
    }

    private record Decision(boolean allowed, Instant resetAt) {
    }
}
