package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.LogRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRecordRepository extends JpaRepository<LogRecord, Long> {

    @Query("""
            SELECT l FROM LogRecord l
            WHERE l.userId = :userId
              AND l.occurredAt >= :startAt
              AND (:targetId IS NULL OR l.targetId = :targetId)
              AND (:hostname IS NULL OR l.hostname = :hostname)
            ORDER BY l.occurredAt DESC
            """)
    List<LogRecord> findIncidentContext(@Param("userId") Long userId,
                                        @Param("targetId") Long targetId,
                                        @Param("hostname") String hostname,
                                        @Param("startAt") LocalDateTime startAt,
                                        Pageable pageable);

    @Modifying
    @Query("delete from LogRecord l where l.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
