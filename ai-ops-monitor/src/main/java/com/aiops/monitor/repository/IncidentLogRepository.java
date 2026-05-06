package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.IncidentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentLogRepository extends JpaRepository<IncidentLog, Long> {
    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime dateTime);

    long countByUserIdAndCreatedAtAfterAndStatus(Long userId, LocalDateTime dateTime, String status);

    List<IncidentLog> findByUserIdAndResolvedAtIsNotNullAndCreatedAtAfter(Long userId, LocalDateTime createdAfter);

    @Query("""
            SELECT i FROM IncidentLog i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:metricName IS NULL OR i.metricName = :metricName)
              AND (:hostname IS NULL OR i.hostname LIKE CONCAT('%', :hostname, '%'))
              AND (:keyword IS NULL OR i.message LIKE CONCAT('%', :keyword, '%'))
            ORDER BY COALESCE(i.lastSeenAt, i.createdAt) DESC
            """)
    Page<IncidentLog> search(@Param("status") String status,
                             @Param("metricName") String metricName,
                             @Param("hostname") String hostname,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    Optional<IncidentLog> findFirstByTargetIdAndMetricNameAndStatusInOrderByCreatedAtDesc(
            Long targetId,
            String metricName,
            List<String> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(COALESCE(i.occurrenceCount, 1)), 0)
            FROM IncidentLog i
            WHERE i.createdAt >= :start
            """)
    long sumOccurrenceByCreatedAtAfter(@Param("start") LocalDateTime start);

    @Query("""
            SELECT COALESCE(SUM(COALESCE(i.suppressedCount, 0)), 0)
            FROM IncidentLog i
            WHERE i.createdAt >= :start
            """)
    long sumSuppressedByCreatedAtAfter(@Param("start") LocalDateTime start);

    @Query("""
            SELECT i FROM IncidentLog i
            WHERE i.status = 'OPEN'
              AND i.nextNotifyAt IS NOT NULL
              AND i.nextNotifyAt <= :now
            ORDER BY i.nextNotifyAt ASC
            """)
    Page<IncidentLog> findEscalationCandidates(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("delete from IncidentLog i where i.createdAt < :before and i.status = 'RESOLVED'")
    int deleteResolvedOlderThan(@Param("before") LocalDateTime before);
}
