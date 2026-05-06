package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.NotificationChannel;
import com.aiops.monitor.model.entity.NotificationDeliveryLog;
import com.aiops.monitor.model.entity.TargetNotificationSubscription;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.NotificationChannelRepository;
import com.aiops.monitor.repository.NotificationDeliveryLogRepository;
import com.aiops.monitor.repository.TargetNotificationSubscriptionRepository;
import com.aiops.monitor.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final NotificationChannelRepository notificationChannelRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final NotificationEmailService notificationEmailService;
    private final UserRepository userRepository;
    private final TargetNotificationSubscriptionRepository targetNotificationSubscriptionRepository;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Async("taskExecutor")
    public void dispatchIncidentOpened(IncidentLog incident) {
        dispatch("INCIDENT_OPENED", incident);
    }

    @Async("taskExecutor")
    public void dispatchIncidentStatusChanged(IncidentLog incident) {
        dispatch("INCIDENT_STATUS_CHANGED", incident);
    }

    @Async("taskExecutor")
    public void dispatchIncidentEscalated(IncidentLog incident) {
        dispatch("INCIDENT_ESCALATED", incident);
    }

    public void dispatchChannelTest(NotificationChannel channel, Long userId) {
        if (channel == null || userId == null) {
            return;
        }
        IncidentLog incident = new IncidentLog();
        incident.setId(0L);
        incident.setUserId(userId);
        incident.setStatus("OPEN");
        incident.setSeverity("P2");
        incident.setMetricName("cpu.usage");
        incident.setMetricValue(92.4);
        incident.setThreshold(85.0);
        incident.setMessage("这是一封测试通知，用于验证通知通道配置是否正确。");
        incident.setHostname("test-node");
        incident.setTargetId(0L);
        incident.setEscalationLevel(0);
        incident.setCreatedAt(LocalDateTime.now());

        if ("WEBHOOK".equalsIgnoreCase(channel.getType())) {
            deliverWebhook(channel, "CHANNEL_TEST", incident);
        } else if ("EMAIL".equalsIgnoreCase(channel.getType())) {
            deliverEmail(channel, "CHANNEL_TEST", incident);
        }
    }

    private void dispatch(String eventType, IncidentLog incident) {
        if (incident == null) {
            return;
        }

        List<NotificationChannel> channels = notificationChannelRepository.findByEnabledTrueOrderByIdDesc();
        for (NotificationChannel channel : channels) {
            if ("WEBHOOK".equalsIgnoreCase(channel.getType())) {
                deliverWebhook(channel, eventType, incident);
            }
        }

        resolveSubscribedUsers(incident).stream()
                .filter(User::isEnabled)
                .filter(User::isNotificationEnabled)
                .filter(user -> user.getNotificationEmail() != null && !user.getNotificationEmail().isBlank())
                .forEach(user -> deliverUserEmail(user, eventType, incident));
    }

    private List<User> resolveSubscribedUsers(IncidentLog incident) {
        if (incident.getTargetId() == null) {
            return List.of();
        }
        List<Long> userIds = targetNotificationSubscriptionRepository.findByTargetIdAndEnabledTrue(incident.getTargetId())
                .stream()
                .map(TargetNotificationSubscription::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds);
    }

    private void deliverWebhook(NotificationChannel channel, String eventType, IncidentLog incident) {
        LocalDateTime now = LocalDateTime.now();
        try {
            String payload = objectMapper.writeValueAsString(buildPayload(eventType, incident));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(channel.getWebhookUrl()))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));

            if (channel.getSecret() != null && !channel.getSecret().isBlank()) {
                builder.header("X-AIOPS-Webhook-Secret", channel.getSecret());
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;

            saveDeliveryLog(channel, incident, success ? "SUCCESS" : "FAILED", response.statusCode(),
                    truncate(response.body(), 1000), null, now);

            channel.setLastNotifiedAt(now);
            channel.setLastError(success ? null : truncate("HTTP " + response.statusCode(), 500));
            channel.setUpdatedAt(now);
            notificationChannelRepository.save(channel);
        } catch (JsonProcessingException e) {
            log.error("Webhook payload serialization failed, channelId={}", channel.getId(), e);
            saveDeliveryLog(channel, incident, "FAILED", null, null, truncate(e.getMessage(), 500), now);
            updateChannelError(channel, now, e.getMessage());
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Webhook delivery failed, channelId={}, incidentId={}", channel.getId(), incident.getId(), e);
            saveDeliveryLog(channel, incident, "FAILED", null, null, truncate(e.getMessage(), 500), now);
            updateChannelError(channel, now, e.getMessage());
        }
    }

    private void deliverEmail(NotificationChannel channel, String eventType, IncidentLog incident) {
        LocalDateTime now = LocalDateTime.now();
        try {
            notificationEmailService.sendIncidentMail(channel, eventType, incident);
            saveDeliveryLog(channel, incident, "SUCCESS", 250, "SMTP accepted", null, now);
            channel.setLastNotifiedAt(now);
            channel.setLastError(null);
            channel.setUpdatedAt(now);
            notificationChannelRepository.save(channel);
        } catch (Exception e) {
            log.warn("Email delivery failed, channelId={}, incidentId={}", channel.getId(), incident.getId(), e);
            saveDeliveryLog(channel, incident, "FAILED", null, null, truncate(e.getMessage(), 500), now);
            updateChannelError(channel, now, e.getMessage());
        }
    }

    private void deliverUserEmail(User user, String eventType, IncidentLog incident) {
        LocalDateTime now = LocalDateTime.now();
        try {
            notificationEmailService.sendIncidentMailToRecipient(user.getNotificationEmail(), eventType, incident);
            saveUserEmailDeliveryLog(user, incident, "SUCCESS", 250, "SMTP accepted", null, now);
        } catch (Exception e) {
            log.warn("User email delivery failed, userId={}, incidentId={}", user.getId(), incident.getId(), e);
            saveUserEmailDeliveryLog(user, incident, "FAILED", null, null, truncate(e.getMessage(), 500), now);
        }
    }

    private Map<String, Object> buildPayload(String eventType, IncidentLog incident) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("occurredAt", LocalDateTime.now());

        Map<String, Object> incidentData = new LinkedHashMap<>();
        incidentData.put("id", incident.getId());
        incidentData.put("status", incident.getStatus());
        incidentData.put("severity", incident.getSeverity());
        incidentData.put("metricName", incident.getMetricName());
        incidentData.put("metricValue", incident.getMetricValue());
        incidentData.put("threshold", incident.getThreshold());
        incidentData.put("message", incident.getMessage());
        incidentData.put("hostname", incident.getHostname());
        incidentData.put("targetId", incident.getTargetId());
        incidentData.put("escalationLevel", incident.getEscalationLevel());
        incidentData.put("lastNotifiedAt", incident.getLastNotifiedAt());
        incidentData.put("nextNotifyAt", incident.getNextNotifyAt());
        incidentData.put("createdAt", incident.getCreatedAt());
        incidentData.put("acknowledgedAt", incident.getAcknowledgedAt());
        incidentData.put("resolvedAt", incident.getResolvedAt());
        payload.put("incident", incidentData);
        return payload;
    }

    private void saveDeliveryLog(NotificationChannel channel,
                                 IncidentLog incident,
                                 String status,
                                 Integer httpStatus,
                                 String responseBody,
                                 String errorMessage,
                                 LocalDateTime createdAt) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setUserId(incident.getUserId());
        log.setIncidentId(incident.getId());
        log.setChannelId(channel.getId());
        log.setChannelName(channel.getName());
        log.setTarget("EMAIL".equalsIgnoreCase(channel.getType()) ? channel.getEmailTo() : channel.getWebhookUrl());
        log.setStatus(status);
        log.setHttpStatus(httpStatus);
        log.setResponseBody(responseBody);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(createdAt);
        notificationDeliveryLogRepository.save(log);
    }

    private void updateChannelError(NotificationChannel channel, LocalDateTime now, String message) {
        channel.setLastNotifiedAt(now);
        channel.setLastError(truncate(message, 500));
        channel.setUpdatedAt(now);
        notificationChannelRepository.save(channel);
    }

    private void saveUserEmailDeliveryLog(User user,
                                          IncidentLog incident,
                                          String status,
                                          Integer httpStatus,
                                          String responseBody,
                                          String errorMessage,
                                          LocalDateTime createdAt) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setUserId(user.getId());
        log.setIncidentId(incident.getId());
        log.setChannelId(null);
        log.setChannelName("USER_EMAIL_PROFILE");
        log.setTarget(user.getNotificationEmail());
        log.setStatus(status);
        log.setHttpStatus(httpStatus);
        log.setResponseBody(responseBody);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(createdAt);
        notificationDeliveryLogRepository.save(log);
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return null;
        }
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
