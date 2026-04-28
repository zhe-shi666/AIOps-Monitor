package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiRollbackRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRollbackRunRepository extends JpaRepository<AiRollbackRun, Long> {
    List<AiRollbackRun> findByInvestigationIdAndUserIdOrderByCreatedAtDesc(Long investigationId, Long userId);
    List<AiRollbackRun> findByActionPlanIdAndInvestigationIdAndUserIdOrderByCreatedAtDesc(Long actionPlanId, Long investigationId, Long userId);
}
