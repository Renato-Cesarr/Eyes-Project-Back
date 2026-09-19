package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.bootstrap;

import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true")
public class InitialAdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminBootstrap.class);
    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    private final InitialAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            LOGGER.info("Initial administrator bootstrap skipped because an administrator already exists");
            return;
        }

        String normalizedEmail = properties.email().trim().toLowerCase(Locale.ROOT);
        User administrator = userRepository.findByEmail(normalizedEmail)
                .map(this::promoteExistingUser)
                .orElseGet(() -> createAdministrator(normalizedEmail));

        userRepository.save(administrator);
        LOGGER.info("Initial administrator provisioned successfully");
    }

    private User promoteExistingUser(User user) {
        user.setRole(UserRole.ADMIN);
        if (Boolean.FALSE.equals(user.getActive()) || user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(properties.password()));
            user.setActive(true);
        }
        return user;
    }

    private User createAdministrator(String normalizedEmail) {
        return User.builder()
                .name(properties.name().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(properties.password()))
                .active(true)
                .role(UserRole.ADMIN)
                .build();
    }

    private void validateConfiguration() {
        if (isBlank(properties.name()) || isBlank(properties.email()) || isBlank(properties.password())) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_NAME, BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD are required when bootstrap is enabled"
            );
        }
        if (!properties.email().contains("@")) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL must be a valid email address");
        }
        if (properties.password().length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 8 characters");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
