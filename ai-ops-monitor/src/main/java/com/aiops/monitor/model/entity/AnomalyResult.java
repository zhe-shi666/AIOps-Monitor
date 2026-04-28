package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_result")
@Data
public class AnomalyResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;

    private String hostname;

    @Column(name = "metric_key", nullable = false)
    private String metricKey;

    private Double score;

    private Double baseline;

    private Double observed;

    private String severity = "MEDIUM";

    private String status = "OPEN";

    @Column(name = "detected_at")
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Column(name = "source_metric_id")
    private Long sourceMetricId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
