package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiReportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiReportSnapshotRepository extends JpaRepository<AiReportSnapshot, Long> {

    List<AiReportSnapshot> findByInvestigationIdOrderByVersionNoDesc(Long investigationId);

    List<AiReportSnapshot> findByInvestigationIdAndUserIdOrderByVersionNoDesc(Long investigationId, Long userId);

    Optional<AiReportSnapshot> findFirstByInvestigationIdOrderByVersionNoDesc(Long investigationId);

    Optional<AiReportSnapshot> findFirstByInvestigationIdAndUserIdOrderByVersionNoDesc(Long investigationId, Long userId);
}
