package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemMetricsRepository extends JpaRepository<SystemMetricsHistory, Long> {
    // 获取最近的 10 条记录
    List<SystemMetricsHistory> findTop10ByOrderByTimestampDesc();

    // 获取最近的 20 条记录用于预测
    List<SystemMetricsHistory> findTop20ByOrderByTimestampDesc();
}