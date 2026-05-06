package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.TraceSpan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TraceSpanRepository extends JpaRepository<TraceSpan, Long> {

    @Query("""
            SELECT t FROM TraceSpan t
            WHERE t.startedAt >= :startAt
              AND (:targetId IS NULL OR t.targetId = :targetId)
              AND (:hostname IS NULL OR t.hostname = :hostname)
            ORDER BY t.startedAt DESC
            """)
    List<TraceSpan> findIncidentContext(@Param("targetId") Long targetId,
                                        @Param("hostname") String hostname,
                                        @Param("startAt") LocalDateTime startAt,
                                        Pageable pageable);

    @Modifying
    @Query("delete from TraceSpan t where t.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
