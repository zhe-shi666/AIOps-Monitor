package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.TargetThresholdConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TargetThresholdConfigRepository extends JpaRepository<TargetThresholdConfig, Long> {
    Optional<TargetThresholdConfig> findByUserIdAndTargetId(Long userId, Long targetId);
}
