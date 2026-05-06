package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_release")
@Data
public class AgentRelease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String version;

    @Column(name = "package_name", nullable = false, length = 128)
    private String packageName;

    @Column(nullable = false, length = 128)
    private String sha256;

    @Column(length = 256)
    private String signature;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(nullable = false)
    private boolean mandatory;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
