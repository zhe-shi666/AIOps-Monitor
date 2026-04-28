package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiActionAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiActionAuditRepository extends JpaRepository<AiActionAudit, Long> {

    @Query("""
            SELECT a FROM AiActionAudit a
            WHERE a.investigationId = :investigationId
              AND a.userId = :userId
              AND (:actionPlanId IS NULL OR a.actionPlanId = :actionPlanId)
              AND (:eventType IS NULL OR a.eventType = :eventType)
            ORDER BY a.createdAt DESC
            """)
    Page<AiActionAudit> search(@Param("investigationId") Long investigationId,
                               @Param("userId") Long userId,
                               @Param("actionPlanId") Long actionPlanId,
                               @Param("eventType") String eventType,
                               Pageable pageable);
}
