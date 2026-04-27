package com.aiops.monitor.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_delivery_log")
@Data
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "channel_name", nullable = false, length = 100)
    private String channelName;

    @Column(name = "target", nullable = false, length = 500)
    private String target;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", length = 1000)
    private String responseBody;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
