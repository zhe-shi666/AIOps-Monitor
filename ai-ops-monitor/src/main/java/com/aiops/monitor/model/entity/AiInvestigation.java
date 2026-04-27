package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_investigation")
@Data
public class AiInvestigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 200)
    private String title;

    @Column(name = "trigger_source", length = 30)
    private String triggerSource = "INCIDENT";

    @Column(length = 30)
    private String status = "COLLECTING";

    @Column(length = 10)
    private String severity = "P2";

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "root_cause", length = 300)
    private String rootCause;

    private Double confidence;

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
