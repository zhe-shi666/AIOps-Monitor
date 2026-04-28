package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "rca_report")
@Data
public class RcaReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "summary_md", columnDefinition = "MEDIUMTEXT")
    private String summaryMd;

    @Column(name = "evidence_json", columnDefinition = "LONGTEXT")
    private String evidenceJson;

    private Double confidence;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_hash")
    private String promptHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
