package br.com.eyesproject.eyes_project_back.modules.accessrequest.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.models.AccessRequestQuery;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccessRequestJpaAdapterH2IntegrationTest {

    @Autowired AccessRequestRepository repository;
    @Autowired SpringDataAccessRequestRepository springDataRepository;

    @BeforeEach
    void setUp() {
        springDataRepository.deleteAll();
        save("Ana", "ana@example.com", AccessRequestStatus.PENDING);
        save("Bruno", "bruno@example.com", AccessRequestStatus.REJECTED);
        save("Carla", "carla@example.com", AccessRequestStatus.PENDING);
    }

    @Test
    @DisplayName("persists, finds and locks requests")
    void persistsAndFindsRequests() {
        AccessRequest pending = repository.findPendingByEmail("ana@example.com").orElseThrow();

        assertEquals("Ana", pending.getName());
        assertEquals(pending.getId(), repository.findById(pending.getId()).orElseThrow().getId());
        assertEquals(pending.getId(), repository.findByIdForUpdate(pending.getId()).orElseThrow().getId());
        assertTrue(repository.findById("invalid-id").isEmpty());
    }

    @Test
    @DisplayName("filters requests and sorts newest first")
    void searchesRequests() {
        var result = repository.search(new AccessRequestQuery(0, 10, "A", AccessRequestStatus.PENDING));

        assertEquals(2, result.totalElements());
        assertEquals(2, result.content().size());
        assertTrue(result.content().stream().allMatch(item -> item.getStatus() == AccessRequestStatus.PENDING));
    }

    private AccessRequest save(String name, String email, AccessRequestStatus status) {
        AccessRequest request = AccessRequest.builder()
                .name(name)
                .email(email)
                .status(status)
                .build();
        if (status == AccessRequestStatus.REJECTED) {
            request.reject("00000000-0000-0000-0000-000000000001", "Dados insuficientes", java.time.LocalDateTime.now());
        }
        return repository.save(request);
    }
}
