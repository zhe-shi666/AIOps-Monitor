package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AlertThresholdConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertThresholdConfigRepository extends JpaRepository<AlertThresholdConfig, Long> {
    Optional<AlertThresholdConfig> findByUserId(Long userId);
}
