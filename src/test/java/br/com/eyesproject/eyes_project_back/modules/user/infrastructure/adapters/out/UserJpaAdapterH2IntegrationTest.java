package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserJpaAdapterH2IntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired SpringDataUserRepository springDataUserRepository;

    @BeforeEach
    void setUp() {
        springDataUserRepository.deleteAll();
        save("Ada Admin", "ada@example.com", UserRole.ADMIN, true);
        save("Bruno Student", "bruno@example.com", UserRole.STUDENT, true);
        save("Carla Student", "carla@example.com", UserRole.STUDENT, false);
    }

    @Test
    void shouldApplyAllSearchFilters() {
        var result = userRepository.search(new UserQuery(
                0, 10, "BRUNO", UserRole.STUDENT, true,
                UserQuery.SortField.NAME, UserQuery.SortDirection.ASC
        ));

        assertEquals(1, result.totalElements());
        assertEquals("bruno@example.com", result.content().getFirst().getEmail());
    }

    @Test
    void shouldPageAndSortWithoutOptionalFilters() {
        var byEmail = userRepository.search(new UserQuery(
                0, 2, null, null, null,
                UserQuery.SortField.EMAIL, UserQuery.SortDirection.DESC
        ));
        var byCreation = userRepository.search(new UserQuery(
                0, 10, null, null, null,
                UserQuery.SortField.CREATED_AT, UserQuery.SortDirection.ASC
        ));

        assertEquals(3, byEmail.totalElements());
        assertEquals(2, byEmail.content().size());
        assertEquals(2, byEmail.totalPages());
        assertEquals("carla@example.com", byEmail.content().getFirst().getEmail());
        assertEquals(3, byCreation.content().size());
    }

    private User save(String name, String email, UserRole role, boolean active) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .active(active)
                .role(role)
                .build());
    }
}
