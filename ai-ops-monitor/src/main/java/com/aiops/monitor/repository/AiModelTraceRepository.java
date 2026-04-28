package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiModelTrace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiModelTraceRepository extends JpaRepository<AiModelTrace, Long> {
    Page<AiModelTrace> findByInvestigationIdAndUserIdOrderByCreatedAtDesc(Long investigationId, Long userId, Pageable pageable);
}
