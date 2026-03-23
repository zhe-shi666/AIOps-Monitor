package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_metrics_history")
@Data
public class SystemMetricsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "mem_usage")
    private Double memUsage;

    private java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();

    private String hostname;
}