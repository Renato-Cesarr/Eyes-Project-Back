package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.global.web.CorrelationIdFilter;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAuditContextProviderTest {

    private final SpringAuditContextProvider provider = new SpringAuditContextProvider();

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void readsAuthenticatedActorAndCorrelationId() {
        User actor = User.builder().id("actor-id").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null)
        );
        MDC.put(CorrelationIdFilter.MDC_KEY, "request-123");

        assertEquals("actor-id", provider.currentActorUserId().orElseThrow());
        assertEquals("request-123", provider.currentCorrelationId().orElseThrow());
    }

    @Test
    void returnsEmptyContextForAnonymousExecution() {
        assertTrue(provider.currentActorUserId().isEmpty());
        assertTrue(provider.currentCorrelationId().isEmpty());
    }
}
