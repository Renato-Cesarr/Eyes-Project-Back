package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.global.exceptions.ResourceNotFoundException;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserUseCaseImplTest {

    @Mock UserRepository userRepository;
    @InjectMocks GetUserUseCaseImpl useCase;

    @Test
    void shouldReturnSafeUserView() {
        User user = User.builder().id("id").name("Ana").email("ana@example.com")
                .active(false).password(null).build();
        when(userRepository.findById("id")).thenReturn(Optional.of(user));

        var result = useCase.execute("id");

        assertEquals("ana@example.com", result.email());
        assertTrue(result.invitationPending());
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute("unknown"));
    }
}
