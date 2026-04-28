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

    @Column(length = 10)
    private String severity = "P2";

    @Column(length = 20)
    private String status = "OPEN";

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "next_notify_at")
    private LocalDateTime nextNotifyAt;

    @Column(name = "fingerprint")
    private String fingerprint;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount = 1;

    @Column(name = "suppressed_count")
    private Integer suppressedCount = 0;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "source_type", length = 30)
    private String sourceType = "METRIC";

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
