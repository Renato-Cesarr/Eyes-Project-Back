package br.com.eyesproject.eyes_project_back.modules.accessrequest.infrastructure.adapters.out;

import br.com.eyesproject.eyes_project_back.modules.accessrequest.domain.models.AccessRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAccessRequestRepository extends
        JpaRepository<AccessRequestJpaEntity, UUID>, JpaSpecificationExecutor<AccessRequestJpaEntity> {

    Optional<AccessRequestJpaEntity> findFirstByEmailAndStatusOrderByCreatedAtDesc(
            String email,
            AccessRequestStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from AccessRequestJpaEntity request where request.id = :id")
    Optional<AccessRequestJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
