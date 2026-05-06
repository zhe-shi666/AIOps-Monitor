package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AnomalyResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AnomalyResultRepository extends JpaRepository<AnomalyResult, Long> {

    @Query("""
            SELECT a FROM AnomalyResult a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:metricKey IS NULL OR a.metricKey = :metricKey)
              AND (:targetId IS NULL OR a.targetId = :targetId)
            ORDER BY a.detectedAt DESC
            """)
    Page<AnomalyResult> search(@Param("status") String status,
                               @Param("metricKey") String metricKey,
                               @Param("targetId") Long targetId,
                               Pageable pageable);

    Optional<AnomalyResult> findFirstByTargetIdAndMetricKeyAndStatusOrderByDetectedAtDesc(
            Long targetId,
            String metricKey,
            String status
    );

    long countByUserIdAndDetectedAtAfter(Long userId, LocalDateTime detectedAt);

    long countByUserIdAndSeverityAndDetectedAtAfter(Long userId, String severity, LocalDateTime detectedAt);

    @Modifying
    @Query("delete from AnomalyResult a where a.createdAt < :before and a.status <> 'OPEN'")
    int deleteClosedOlderThan(@Param("before") LocalDateTime before);
}
