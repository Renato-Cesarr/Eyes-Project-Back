package br.com.eyesproject.eyes_project_back.modules.user.application.ports.out;

import br.com.eyesproject.eyes_project_back.modules.user.application.models.PageResult;
import br.com.eyesproject.eyes_project_back.modules.user.application.models.UserQuery;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.User;
import br.com.eyesproject.eyes_project_back.modules.user.domain.models.UserRole;
import java.util.Optional;

/**
 * Output port defining what the domain expects from the infrastructure.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByIdForUpdate(String id);
    PageResult<User> search(UserQuery query);
    long countActiveByRoleForUpdate(UserRole role);
    boolean existsByRole(UserRole role);
}
