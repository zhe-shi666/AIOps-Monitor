package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_rollback_run")
@Data
public class AiRollbackRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_plan_id", nullable = false)
    private Long actionPlanId;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String executor;

    @Column(name = "drill_mode")
    private boolean drillMode = false;

    @Column(name = "execution_mode")
    private String executionMode = "MANUAL";

    private String status = "PENDING";

    @Column(name = "note_text", columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "output_text", columnDefinition = "MEDIUMTEXT")
    private String outputText;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
