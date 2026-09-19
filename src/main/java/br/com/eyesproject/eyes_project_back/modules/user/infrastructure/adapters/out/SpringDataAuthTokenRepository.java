package br.com.eyesproject.eyes_project_back.modules.user.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.user.domain.models.TokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAuthTokenRepository extends JpaRepository<AuthTokenJpaEntity, UUID> {
    Optional<AuthTokenJpaEntity> findByTokenAndType(String token, TokenType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuthTokenJpaEntity a JOIN FETCH a.user WHERE a.token = :token AND a.type = :type")
    Optional<AuthTokenJpaEntity> findByTokenAndTypeForUpdate(
            @Param("token") String token,
            @Param("type") TokenType type
    );
    
    @Transactional
    @Modifying
    @Query("DELETE FROM AuthTokenJpaEntity a WHERE a.user.id = :userId AND a.type = :type")
    void deleteByUserIdAndType(UUID userId, TokenType type);
}
