package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import br.com.eyesproject.eyes_project_back.utils.factories.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.List;
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
                .role(domainUser.getRole())
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
        assertEquals(domainUser.getRole(), result.getRole());
        verify(springDataUserRepository, times(1)).save(any(UserJpaEntity.class));
    }

    @Test
    @DisplayName("Should find user by email and map to domain")
    void shouldFindUserByEmail() {
        // Arrange
        when(springDataUserRepository.findByEmailIgnoreCase(domainUser.getEmail())).thenReturn(Optional.of(userEntity));

        // Act
        Optional<User> result = userJpaAdapter.findByEmail(domainUser.getEmail());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(domainUser.getEmail(), result.get().getEmail());
        assertEquals(domainUser.getRole(), result.get().getRole());
        verify(springDataUserRepository, times(1)).findByEmailIgnoreCase(domainUser.getEmail());
    }

    @Test
    @DisplayName("Should return empty Optional when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // Arrange
        when(springDataUserRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userJpaAdapter.findByEmail("notfound@test.com");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should check whether a user role exists")
    void shouldCheckWhetherRoleExists() {
        when(springDataUserRepository.existsByRole(domainUser.getRole())).thenReturn(true);

        assertTrue(userJpaAdapter.existsByRole(domainUser.getRole()));
        verify(springDataUserRepository).existsByRole(domainUser.getRole());
    }

    @Test
    void shouldReturnEmptyForInvalidUserIdWithoutQueryingDatabase() {
        assertTrue(userJpaAdapter.findById("invalid-id").isEmpty());
        assertTrue(userJpaAdapter.findByIdForUpdate("invalid-id").isEmpty());
        verifyNoInteractions(springDataUserRepository);
    }

    @Test
    void shouldFindUserWithWriteLock() {
        UUID id = UUID.fromString(domainUser.getId());
        when(springDataUserRepository.findByIdForUpdate(id)).thenReturn(Optional.of(userEntity));

        assertTrue(userJpaAdapter.findByIdForUpdate(domainUser.getId()).isPresent());
        verify(springDataUserRepository).findByIdForUpdate(id);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSearchUsersWithSafePaginationAndSorting() {
        var query = new UserQuery(0, 10, "ana", UserRole.STUDENT, true,
                UserQuery.SortField.EMAIL, UserQuery.SortDirection.DESC);
        when(springDataUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userEntity)));

        var result = userJpaAdapter.search(query);

        assertEquals(1, result.totalElements());
        assertEquals(domainUser.getEmail(), result.content().getFirst().getEmail());
    }

    @Test
    void shouldLockAndCountActiveAdministrators() {
        when(springDataUserRepository.findActiveByRoleForUpdate(UserRole.ADMIN))
                .thenReturn(List.of(userEntity));
        assertEquals(1, userJpaAdapter.countActiveByRoleForUpdate(UserRole.ADMIN));
    }
}
