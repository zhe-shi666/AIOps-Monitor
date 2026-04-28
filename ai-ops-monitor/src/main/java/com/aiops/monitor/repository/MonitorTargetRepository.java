package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.MonitorTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MonitorTargetRepository extends JpaRepository<MonitorTarget, Long> {
    List<MonitorTarget> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MonitorTarget> findByIdAndUserId(Long id, Long userId);
    Optional<MonitorTarget> findFirstByUserIdAndHostnameOrderByCreatedAtDesc(Long userId, String hostname);
    Optional<MonitorTarget> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MonitorTarget> findByAgentKey(String agentKey);
    long countByUserId(Long userId);
    List<MonitorTarget> findByEnabledTrueAndLastHeartbeatAtBefore(LocalDateTime threshold);
}
