package br.com.eyesproject.eyes_project_back.modules.audit.infrastructure.adapters.out;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
    List<AuditLogJpaEntity> findByActorUserIdOrderByOccurredAtDesc(UUID actorUserId);
}
