package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiHypothesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiHypothesisRepository extends JpaRepository<AiHypothesis, Long> {

    List<AiHypothesis> findByInvestigationIdOrderByRankOrderAsc(Long investigationId);

    List<AiHypothesis> findByInvestigationIdAndUserIdOrderByRankOrderAsc(Long investigationId, Long userId);
}
