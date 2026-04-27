package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_log")
@Data
public class IncidentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name")
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue;

    private Double threshold;

    private String message;

    private String hostname;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
