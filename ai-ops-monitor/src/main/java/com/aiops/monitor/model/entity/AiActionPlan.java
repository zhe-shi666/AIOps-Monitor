package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_action_plan")
@Data
public class AiActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "hypothesis_id")
    private Long hypothesisId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "command_text", columnDefinition = "text")
    private String commandText;

    @Column(name = "runbook_ref", length = 500)
    private String runbookRef;

    @Column(name = "risk_level", length = 20)
    private String riskLevel = "MEDIUM";

    @Column(name = "requires_approval")
    private boolean requiresApproval = true;

    @Column(length = 20)
    private String status = "PROPOSED";

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_note", columnDefinition = "text")
    private String approvalNote;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "rollback_plan", columnDefinition = "text")
    private String rollbackPlan;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
