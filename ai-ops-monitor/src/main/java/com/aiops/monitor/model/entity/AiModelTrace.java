package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_model_trace")
@Data
public class AiModelTrace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "investigation_id")
    private Long investigationId;

    private String phase;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "prompt_text", columnDefinition = "LONGTEXT")
    private String promptText;

    @Column(name = "response_text", columnDefinition = "LONGTEXT")
    private String responseText;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "response_tokens")
    private Integer responseTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    private String status = "SUCCESS";

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
