package br.com.eyesproject.eyes_project_back.modules.user.application.services;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.application.ports.out.UserRepository;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchUsersUseCaseImplTest {

    @Mock UserRepository userRepository;
    @InjectMocks SearchUsersUseCaseImpl useCase;

    @Test
    void shouldMapDomainPageToSafeViews() {
        UserQuery query = new UserQuery(0, 20, " ana ", null, true, null, null);
        User user = User.builder().id("id").name("Ana").email("ana@example.com")
                .active(true).password("secret-hash").build();
        when(userRepository.search(query)).thenReturn(new PageResult<>(List.of(user), 0, 20, 1, 1));

        var result = useCase.execute(query);

        assertEquals(1, result.totalElements());
        assertEquals("ana@example.com", result.content().getFirst().email());
        assertFalse(result.content().getFirst().invitationPending());
    }
}
