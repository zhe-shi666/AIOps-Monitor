package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_report_snapshot")
@Data
public class AiReportSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investigation_id", nullable = false)
    private Long investigationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 1;

    @Column(nullable = false, length = 20)
    private String format = "MARKDOWN";

    @Column(name = "report_markdown", columnDefinition = "mediumtext")
    private String reportMarkdown;

    @Column(name = "report_json", columnDefinition = "longtext")
    private String reportJson;

    @Column(name = "created_by", length = 100)
    private String createdBy = "AI";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
