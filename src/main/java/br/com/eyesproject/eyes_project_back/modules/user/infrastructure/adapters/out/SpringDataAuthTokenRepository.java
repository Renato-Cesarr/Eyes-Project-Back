package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAuthTokenRepository extends JpaRepository<AuthTokenJpaEntity, UUID> {
    Optional<AuthTokenJpaEntity> findByTokenAndType(String token, TokenType type);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM AuthTokenJpaEntity a WHERE a.user.id = :userId AND a.type = :type")
    void deleteByUserIdAndType(UUID userId, TokenType type);
}
