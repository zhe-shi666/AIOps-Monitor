package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiActionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiActionPlanRepository extends JpaRepository<AiActionPlan, Long> {

    List<AiActionPlan> findByInvestigationIdOrderByCreatedAtAsc(Long investigationId);

    List<AiActionPlan> findByInvestigationIdAndUserIdOrderByCreatedAtAsc(Long investigationId, Long userId);

    Optional<AiActionPlan> findByIdAndInvestigationIdAndUserId(Long id, Long investigationId, Long userId);
}
