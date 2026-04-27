package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_threshold_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_alert_threshold_user", columnNames = "user_id"))
@Data
public class AlertThresholdConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cpu_threshold", nullable = false)
    private Double cpuThreshold;

    @Column(name = "memory_threshold", nullable = false)
    private Double memoryThreshold;

    @Column(name = "disk_threshold", nullable = false)
    private Double diskThreshold;

    @Column(name = "process_count_threshold", nullable = false)
    private Integer processCountThreshold;

    @Column(name = "consecutive_breach_count", nullable = false)
    private Integer consecutiveBreachCount;

    @Column(name = "silence_seconds", nullable = false)
    private Integer silenceSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
