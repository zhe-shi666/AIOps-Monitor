package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "target_threshold_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_target_threshold_target", columnNames = "target_id"))
@Data
public class TargetThresholdConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "cpu_threshold")
    private Double cpuThreshold;

    @Column(name = "memory_threshold")
    private Double memoryThreshold;

    @Column(name = "disk_threshold")
    private Double diskThreshold;

    @Column(name = "process_count_threshold")
    private Integer processCountThreshold;

    @Column(name = "consecutive_breach_count")
    private Integer consecutiveBreachCount;

    @Column(name = "silence_seconds")
    private Integer silenceSeconds;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
