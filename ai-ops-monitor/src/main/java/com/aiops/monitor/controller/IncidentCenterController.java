package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.IncidentStatusUpdateRequest;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.LogRecord;
import com.aiops.monitor.model.entity.NotificationDeliveryLog;
import com.aiops.monitor.model.entity.TraceSpan;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.LogRecordRepository;
import com.aiops.monitor.repository.NotificationDeliveryLogRepository;
import com.aiops.monitor.repository.TraceSpanRepository;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.EscalationPolicyService;
import com.aiops.monitor.service.NotificationDispatcherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentCenterController {

    private final IncidentLogRepository incidentLogRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final LogRecordRepository logRecordRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final CurrentUserService currentUserService;
    private final EscalationPolicyService escalationPolicyService;
    private final NotificationDispatcherService notificationDispatcherService;

    @GetMapping
    public ResponseEntity<Page<IncidentLog>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<IncidentLog> incidents = incidentLogRepository.searchByUserId(
                user.getId(),
                normalizeUpper(status),
                normalizeUpper(metricName),
                normalize(hostname),
                normalize(keyword),
                pageable
        );
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/aggregation/summary")
    public ResponseEntity<Map<String, Object>> aggregationSummary(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        int windowDays = Math.max(1, Math.min(365, days));
        LocalDateTime start = LocalDateTime.now().minusDays(windowDays);

        long incidents = incidentLogRepository.countByUserIdAndCreatedAtAfter(user.getId(), start);
        long occurrences = incidentLogRepository.sumOccurrenceByUserIdAndCreatedAtAfter(user.getId(), start);
        long suppressed = incidentLogRepository.sumSuppressedByUserIdAndCreatedAtAfter(user.getId(), start);
        long deduplicated = Math.max(0, occurrences - incidents);
        double reductionRate = occurrences <= 0
                ? 0d
                : ((deduplicated + suppressed) * 100d / occurrences);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowDays", windowDays);
        result.put("incidentCount", incidents);
        result.put("rawOccurrenceCount", occurrences);
        result.put("deduplicatedCount", deduplicated);
        result.put("suppressedCount", suppressed);
        result.put("noiseReductionRate", round(reductionRate));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody IncidentStatusUpdateRequest request,
                                                            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        IncidentLog incident = incidentLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "告警事件不存在"));
        String beforeStatus = incident.getStatus();

        String status = request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!"OPEN".equals(status) && !"ACKNOWLEDGED".equals(status) && !"RESOLVED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅支持 OPEN/ACKNOWLEDGED/RESOLVED");
        }

        incident.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        if ("ACKNOWLEDGED".equals(status)) {
            incident.setAcknowledgedAt(now);
            incident.setNextNotifyAt(null);
        }
        if ("RESOLVED".equals(status)) {
            if (incident.getAcknowledgedAt() == null) {
                incident.setAcknowledgedAt(now);
            }
            incident.setResolvedAt(now);
            incident.setNextNotifyAt(null);
        }
        if ("OPEN".equals(status)) {
            incident.setAcknowledgedAt(null);
            incident.setResolvedAt(null);
            incident.setEscalationLevel(0);
            incident.setLastNotifiedAt(now);
            Integer firstInterval = escalationPolicyService.getIntervalMinutes(
                    escalationPolicyService.getOrCreateByUserId(user.getId()),
                    incident.getSeverity(),
                    0
            );
            incident.setNextNotifyAt(firstInterval == null ? null : now.plusMinutes(firstInterval));
        }

        incidentLogRepository.save(incident);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", incident.getId());
        response.put("status", incident.getStatus());
        response.put("severity", incident.getSeverity());
        response.put("escalationLevel", incident.getEscalationLevel());
        response.put("nextNotifyAt", incident.getNextNotifyAt());
        response.put("acknowledgedAt", incident.getAcknowledgedAt());
        response.put("resolvedAt", incident.getResolvedAt());

        if (beforeStatus == null || !beforeStatus.equals(incident.getStatus())) {
            notificationDispatcherService.dispatchIncidentStatusChanged(incident);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/deliveries")
    public ResponseEntity<Page<NotificationDeliveryLog>> listDeliveries(@PathVariable Long id,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size,
                                                                         Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        incidentLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "告警事件不存在"));
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationDeliveryLog> deliveryLogs = notificationDeliveryLogRepository
                .findByUserIdAndIncidentId(user.getId(), id, pageable);
        return ResponseEntity.ok(deliveryLogs);
    }

    @GetMapping("/{id}/context")
    public ResponseEntity<Map<String, Object>> context(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "30") int minutes,
                                                       @RequestParam(defaultValue = "50") int limit,
                                                       Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        IncidentLog incident = incidentLogRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "告警事件不存在"));
        int contextMinutes = Math.max(5, Math.min(1440, minutes));
        int contextLimit = Math.max(1, Math.min(200, limit));
        LocalDateTime startAt = (incident.getCreatedAt() == null ? LocalDateTime.now() : incident.getCreatedAt())
                .minusMinutes(contextMinutes);
        PageRequest contextPageable = PageRequest.of(0, contextLimit);

        List<LogRecord> logs = logRecordRepository.findIncidentContext(
                user.getId(),
                incident.getTargetId(),
                normalize(incident.getHostname()),
                startAt,
                contextPageable
        );
        List<TraceSpan> traces = traceSpanRepository.findIncidentContext(
                user.getId(),
                incident.getTargetId(),
                normalize(incident.getHostname()),
                startAt,
                contextPageable
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("incidentId", incident.getId());
        result.put("hostname", incident.getHostname());
        result.put("targetId", incident.getTargetId());
        result.put("windowMinutes", contextMinutes);
        result.put("logCount", logs.size());
        result.put("traceCount", traces.size());
        result.put("logs", logs);
        result.put("traces", traces);
        return ResponseEntity.ok(result);
    }

    private String normalize(String input) {
        if (input == null) return null;
        String value = input.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeUpper(String input) {
        if (input == null) return null;
        String value = input.trim();
        return value.isEmpty() ? null : value.toUpperCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
