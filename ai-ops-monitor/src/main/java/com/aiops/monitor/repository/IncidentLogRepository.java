package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.IncidentLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IncidentLogRepository extends JpaRepository<IncidentLog, Long> {
    long countByCreatedAtAfter(LocalDateTime dateTime);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime dateTime);

    @Query("""
            SELECT i FROM IncidentLog i
            WHERE i.userId = :userId
              AND (:status IS NULL OR i.status = :status)
              AND (:metricName IS NULL OR i.metricName = :metricName)
              AND (:hostname IS NULL OR i.hostname LIKE CONCAT('%', :hostname, '%'))
              AND (:keyword IS NULL OR i.message LIKE CONCAT('%', :keyword, '%'))
            ORDER BY i.createdAt DESC
            """)
    Page<IncidentLog> searchByUserId(@Param("userId") Long userId,
                                     @Param("status") String status,
                                     @Param("metricName") String metricName,
                                     @Param("hostname") String hostname,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);

    @Query("""
            SELECT i FROM IncidentLog i
            WHERE i.status = 'OPEN'
              AND i.nextNotifyAt IS NOT NULL
              AND i.nextNotifyAt <= :now
            ORDER BY i.nextNotifyAt ASC
            """)
    Page<IncidentLog> findEscalationCandidates(@Param("now") LocalDateTime now, Pageable pageable);

    Optional<IncidentLog> findByIdAndUserId(Long id, Long userId);
}
