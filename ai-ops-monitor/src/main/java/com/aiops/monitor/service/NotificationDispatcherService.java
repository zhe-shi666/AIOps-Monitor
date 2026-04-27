package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.NotificationChannel;
import com.aiops.monitor.model.entity.NotificationDeliveryLog;
import com.aiops.monitor.repository.NotificationChannelRepository;
import com.aiops.monitor.repository.NotificationDeliveryLogRepository;
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

    private void dispatch(String eventType, IncidentLog incident) {
        if (incident == null || incident.getUserId() == null) {
            return;
        }

        List<NotificationChannel> channels = notificationChannelRepository
                .findByUserIdAndEnabledTrueOrderByIdDesc(incident.getUserId());
        if (channels.isEmpty()) {
            return;
        }

        for (NotificationChannel channel : channels) {
            if (!"WEBHOOK".equalsIgnoreCase(channel.getType())) {
                continue;
            }
            deliverWebhook(channel, eventType, incident);
        }
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

    private Map<String, Object> buildPayload(String eventType, IncidentLog incident) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("occurredAt", LocalDateTime.now());

        Map<String, Object> incidentData = new LinkedHashMap<>();
        incidentData.put("id", incident.getId());
        incidentData.put("status", incident.getStatus());
        incidentData.put("metricName", incident.getMetricName());
        incidentData.put("metricValue", incident.getMetricValue());
        incidentData.put("threshold", incident.getThreshold());
        incidentData.put("message", incident.getMessage());
        incidentData.put("hostname", incident.getHostname());
        incidentData.put("targetId", incident.getTargetId());
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
        log.setTarget(channel.getWebhookUrl());
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
