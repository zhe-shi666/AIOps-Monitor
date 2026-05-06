package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetricsHistory, Long> {

    // 分布式模式：获取全集群最近20条数据
    List<SystemMetricsHistory> findTop20ByOrderByTimestampDesc();

    // 本地模式：仅获取当前节点最近20条数据
    List<SystemMetricsHistory> findTop20ByHostnameOrderByTimestampDesc(String hostname);

    List<SystemMetricsHistory> findTop80ByTargetIdOrderByTimestampDesc(Long targetId);

    @Modifying
    @Query("delete from SystemMetricsHistory h where h.timestamp < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
