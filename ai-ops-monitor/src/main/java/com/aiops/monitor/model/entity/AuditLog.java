package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 100)
    private String actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "resource_type", length = 80)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "detail_json", columnDefinition = "LONGTEXT")
    private String detailJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
