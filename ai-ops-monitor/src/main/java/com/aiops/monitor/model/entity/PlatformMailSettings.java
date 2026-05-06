package com.aiops.monitor.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_mail_settings")
@Data
public class PlatformMailSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "smtp_host", nullable = false, length = 255)
    private String smtpHost;

    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort = 587;

    @Column(name = "smtp_username", nullable = false, length = 255)
    private String smtpUsername;

    @Column(name = "smtp_password", nullable = false, length = 500)
    @JsonIgnore
    private String smtpPassword;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(name = "smtp_starttls", nullable = false)
    private boolean smtpStarttls = true;

    @Column(name = "from_address", nullable = false, length = 255)
    private String fromAddress;

    @Column(name = "from_name", length = 100)
    private String fromName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
