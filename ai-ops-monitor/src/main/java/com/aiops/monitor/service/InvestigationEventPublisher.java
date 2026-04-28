package com.aiops.monitor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvestigationEventPublisher {

    private final MetricsPublisher metricsPublisher;

    public void publish(Long userId, String eventType, Long investigationId, String message, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        // Redis generic serializer stores values as Object; use ISO string to avoid LocalDateTime type-id serialization issues.
        event.put("time", LocalDateTime.now().toString());
        event.put("userId", userId);
        event.put("eventType", eventType);
        event.put("investigationId", investigationId);
        event.put("message", message);
        event.put("payload", payload == null ? Map.of() : payload);
        metricsPublisher.send("/topic/investigations", event);
    }
}
