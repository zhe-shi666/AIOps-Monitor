package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiActionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiActionRunRepository extends JpaRepository<AiActionRun, Long> {

    List<AiActionRun> findByInvestigationIdOrderByCreatedAtAsc(Long investigationId);

    List<AiActionRun> findByInvestigationIdAndUserIdOrderByCreatedAtAsc(Long investigationId, Long userId);
}
