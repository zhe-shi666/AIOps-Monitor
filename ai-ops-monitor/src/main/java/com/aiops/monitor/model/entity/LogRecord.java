package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_record")
@Data
public class LogRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_id")
    private Long targetId;

    private String hostname;

    @Column(name = "service_name")
    private String serviceName;

    private String source;

    private String level;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "labels_json", columnDefinition = "LONGTEXT")
    private String labelsJson;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
