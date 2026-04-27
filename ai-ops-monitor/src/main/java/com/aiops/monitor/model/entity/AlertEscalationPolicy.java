package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_escalation_policy",
        uniqueConstraints = @UniqueConstraint(name = "uk_alert_escalation_user", columnNames = "user_id"))
@Data
public class AlertEscalationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "p1_intervals", nullable = false, length = 100)
    private String p1Intervals;

    @Column(name = "p2_intervals", nullable = false, length = 100)
    private String p2Intervals;

    @Column(name = "p3_intervals", nullable = false, length = 100)
    private String p3Intervals;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
