package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_action_run")
@Data
public class AiActionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_plan_id", nullable = false)
    private Long actionPlanId;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 100)
    private String executor;

    @Column(name = "execution_mode", length = 20)
    private String executionMode = "MANUAL";

    @Column(length = 20)
    private String status = "PENDING";

    @Column(name = "output_text", columnDefinition = "mediumtext")
    private String outputText;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
