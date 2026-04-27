package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "monitor_targets")
@Data
public class MonitorTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String hostname;

    @Column(name = "agent_key", nullable = false, unique = true, length = 64)
    private String agentKey;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(length = 20)
    private String status = "OFFLINE";

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "agent_version", length = 64)
    private String agentVersion;
}
