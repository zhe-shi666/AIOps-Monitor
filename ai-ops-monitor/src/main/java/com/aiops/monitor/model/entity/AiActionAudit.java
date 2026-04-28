package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_action_audit")
@Data
public class AiActionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "action_plan_id", nullable = false)
    private Long actionPlanId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    private String actor;

    private String decision;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "detail_json", columnDefinition = "LONGTEXT")
    private String detailJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
