package br.com.eyesproject.eyes_project_back.global.security.ratelimit;

import br.com.eyesproject.eyes_project_back.global.exceptions.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PublicEndpointRateLimitInterceptor implements HandlerInterceptor {

    static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final int CLEANUP_INTERVAL = 256;

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, ClientBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestsSinceCleanup = new AtomicInteger();
    private final long retentionNanos;

    public PublicEndpointRateLimitInterceptor(RateLimitProperties properties) {
        this.properties = properties;
        long longestWindowSeconds = properties.policies().stream()
                .mapToLong(RateLimitProperties.Policy::windowSeconds)
                .max()
                .orElseThrow();
        this.retentionNanos = Duration.ofSeconds(longestWindowSeconds).multipliedBy(2).toNanos();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<ResolvedPolicy> resolvedPolicy = resolvePolicy(request);
        if (resolvedPolicy.isEmpty()) {
            return true;
        }

        long now = System.nanoTime();
        ResolvedPolicy resolved = resolvedPolicy.get();
        String key = resolved.id() + ':' + request.getRemoteAddr();
        ClientBucket clientBucket = buckets.computeIfAbsent(key,
                ignored -> new ClientBucket(newBucket(resolved.policy()), now));
        clientBucket.touch(now);

        ConsumptionProbe probe = clientBucket.bucket().tryConsumeAndReturnRemaining(1);
        response.setHeader(REMAINING_HEADER, Long.toString(probe.getRemainingTokens()));
        cleanupInactiveBuckets(now);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1,
                    TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
            throw new TooManyRequestsException(
                    "Muitas tentativas. Aguarde antes de tentar novamente.", retryAfterSeconds);
        }
        return true;
    }

    private Optional<ResolvedPolicy> resolvePolicy(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return Optional.empty();
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return Optional.ofNullable(Map.of(
                "/api/v1/auth/login", new ResolvedPolicy("login", properties.login()),
                "/api/v1/access-requests", new ResolvedPolicy("access-request", properties.accessRequest()),
                "/api/v1/auth/forgot-password", new ResolvedPolicy("password-recovery", properties.passwordRecovery()),
                "/api/v1/auth/reset-password", new ResolvedPolicy("password-recovery", properties.passwordRecovery())
        ).get(path));
    }

    private Bucket newBucket(RateLimitProperties.Policy policy) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillGreedy(policy.capacity(), policy.window())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void cleanupInactiveBuckets(long now) {
        if (requestsSinceCleanup.incrementAndGet() < CLEANUP_INTERVAL) {
            return;
        }
        requestsSinceCleanup.set(0);
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccessNanos() >= retentionNanos);
    }

    private record ResolvedPolicy(String id, RateLimitProperties.Policy policy) {
    }

    private static final class ClientBucket {
        private final Bucket bucket;
        private volatile long lastAccessNanos;

        private ClientBucket(Bucket bucket, long lastAccessNanos) {
            this.bucket = bucket;
            this.lastAccessNanos = lastAccessNanos;
        }

        private Bucket bucket() {
            return bucket;
        }

        private long lastAccessNanos() {
            return lastAccessNanos;
        }

        private void touch(long now) {
            this.lastAccessNanos = now;
        }
    }
}
