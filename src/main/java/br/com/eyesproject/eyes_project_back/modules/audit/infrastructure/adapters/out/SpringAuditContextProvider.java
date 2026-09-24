package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.global.web.CorrelationIdFilter;
import br.com.eyesproject.eyes_project_back.modules.audit.application.ports.out.AuditContextProvider;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringAuditContextProvider implements AuditContextProvider {

    @Override
    public Optional<String> currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return Optional.empty();
        }
        return Optional.ofNullable(user.getId()).filter(id -> !id.isBlank());
    }

    @Override
    public Optional<String> currentCorrelationId() {
        return Optional.ofNullable(MDC.get(CorrelationIdFilter.MDC_KEY)).filter(id -> !id.isBlank());
    }
}
