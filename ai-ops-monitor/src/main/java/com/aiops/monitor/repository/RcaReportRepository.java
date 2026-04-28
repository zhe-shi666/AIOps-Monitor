package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.RcaReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RcaReportRepository extends JpaRepository<RcaReport, Long> {
    Optional<RcaReport> findFirstByInvestigationIdAndUserIdOrderByCreatedAtDesc(Long investigationId, Long userId);

    List<RcaReport> findByInvestigationIdAndUserIdOrderByCreatedAtDesc(Long investigationId, Long userId, Pageable pageable);
}
