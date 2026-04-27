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

    @Column(name = "disk_usage")
    private Double diskUsage;

    @Column(name = "net_rx_bytes_per_sec")
    private Double netRxBytesPerSec;

    @Column(name = "net_tx_bytes_per_sec")
    private Double netTxBytesPerSec;

    @Column(name = "process_count")
    private Integer processCount;

    private java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();

    private String hostname;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;
}
