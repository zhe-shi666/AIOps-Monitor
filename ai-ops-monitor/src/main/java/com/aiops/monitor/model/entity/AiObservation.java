package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_observation")
@Data
public class AiObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @Column(length = 255)
    private String hostname;

    @Column(name = "metric_name", length = 50)
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    private Double confidence;

    @Column(name = "payload_json", columnDefinition = "longtext")
    private String payloadJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
