package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.userId = :userId)
              AND (:action IS NULL OR a.action = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
              AND (
                :keyword IS NULL OR
                LOWER(COALESCE(a.actor, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(COALESCE(a.detailJson, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(@Param("userId") Long userId,
                          @Param("action") String action,
                          @Param("resourceType") String resourceType,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    @Modifying
    @Query("delete from AuditLog a where a.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
