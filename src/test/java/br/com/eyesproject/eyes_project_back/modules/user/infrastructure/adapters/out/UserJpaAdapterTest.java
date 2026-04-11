package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserJpaAdapterTest {

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    @InjectMocks
    private UserJpaAdapter userJpaAdapter;

    private User domainUser;
    private UserJpaEntity userEntity;

    @BeforeEach
    void setUp() {
        domainUser = UserFactory.createValidUser();
        userEntity = UserJpaEntity.builder()
                .id(UUID.fromString(domainUser.getId()))
                .name(domainUser.getName())
                .email(domainUser.getEmail())
                .password(domainUser.getPassword())
                .active(domainUser.getActive())
                .createdAt(domainUser.getCreatedAt())
                .updatedAt(domainUser.getUpdatedAt())
                .build();
    }

    @Test
    @DisplayName("Should save user domain and return it mapped from entity")
    void shouldSaveUserSuccessfully() {
        // Arrange
        when(springDataUserRepository.save(any(UserJpaEntity.class))).thenReturn(userEntity);

        // Act
        User result = userJpaAdapter.save(domainUser);

        // Assert
        assertNotNull(result);
        assertEquals(domainUser.getEmail(), result.getEmail());
        verify(springDataUserRepository, times(1)).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("Should find user by email and map to domain")
    void shouldFindUserByEmail() {
        // Arrange
        when(springDataUserRepository.findByEmail(domainUser.getEmail())).thenReturn(Optional.of(userEntity));

        // Act
        Optional<User> result = userJpaAdapter.findByEmail(domainUser.getEmail());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(domainUser.getEmail(), result.get().getEmail());
        verify(springDataUserRepository, times(1)).findByEmail(domainUser.getEmail());
    }

    @Test
    @DisplayName("Should return empty Optional when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // Arrange
        when(springDataUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userJpaAdapter.findByEmail("notfound@test.com");

        // Assert
        assertTrue(result.isEmpty());
    }
}
