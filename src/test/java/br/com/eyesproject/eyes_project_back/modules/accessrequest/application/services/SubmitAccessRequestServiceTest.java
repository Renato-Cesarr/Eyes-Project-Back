package br.com.eyesproject.eyes_project_back.modules.accessrequest.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ConflictException;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.application.ports.out.AccessRequestRepository;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequest;
import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitAccessRequestServiceTest {

    @Mock AccessRequestRepository accessRequestRepository;
    @Mock UserRepository userRepository;
    @InjectMocks SubmitAccessRequestService service;

    @Test
    @DisplayName("creates a normalized pending request")
    void createsNormalizedPendingRequest() {
        AccessRequest input = input("  Ana   Silva ", " ANA@EXAMPLE.COM ");
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(accessRequestRepository.findPendingByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(accessRequestRepository.save(any(AccessRequest.class))).thenAnswer(invocation -> {
            AccessRequest saved = invocation.getArgument(0, AccessRequest.class);
            saved.setId("request-id");
            return saved;
        });

        var result = service.execute(input);

        assertTrue(result.created());
        assertEquals("request-id", result.request().getId());
        ArgumentCaptor<AccessRequest> captor = ArgumentCaptor.forClass(AccessRequest.class);
        verify(accessRequestRepository).save(captor.capture());
        assertEquals("Ana Silva", captor.getValue().getName());
        assertEquals("ana@example.com", captor.getValue().getEmail());
        assertEquals("Preciso acompanhar as aulas", captor.getValue().getRequestReason());
        assertEquals(AccessRequestStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("returns the existing pending request without creating a duplicate")
    void returnsExistingPendingRequest() {
        AccessRequest existing = AccessRequest.builder().id("existing").status(AccessRequestStatus.PENDING).build();
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(accessRequestRepository.findPendingByEmail("ana@example.com")).thenReturn(Optional.of(existing));

        var result = service.execute(input("Ana", "ana@example.com"));

        assertFalse(result.created());
        assertEquals(existing, result.request());
        verify(accessRequestRepository, never()).save(any(AccessRequest.class));
    }

    @Test
    @DisplayName("rejects an email that already belongs to a user")
    void rejectsRegisteredEmail() {
        when(userRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(User.builder().email("ana@example.com").build()));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.execute(input("Ana", "ANA@example.com"))
        );

        assertEquals("Não é possível criar uma solicitação para este e-mail", exception.getMessage());
        verify(accessRequestRepository, never()).save(any(AccessRequest.class));
    }

    @Test
    @DisplayName("turns a concurrent unique-key conflict into an idempotent response")
    void handlesConcurrentSubmission() {
        AccessRequest existing = AccessRequest.builder().id("winner").status(AccessRequestStatus.PENDING).build();
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(accessRequestRepository.findPendingByEmail("ana@example.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(accessRequestRepository.save(any(AccessRequest.class)))
                .thenThrow(new ConflictException("Já existe uma solicitação pendente para este e-mail"));

        var result = service.execute(input("Ana", "ana@example.com"));

        assertFalse(result.created());
        assertEquals("winner", result.request().getId());
    }

    private AccessRequest input(String name, String email) {
        return AccessRequest.builder()
                .name(name)
                .email(email)
                .requestReason("  Preciso acompanhar as aulas  ")
                .build();
    }
}
