package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.IncidentStatusUpdateRequest;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.NotificationDeliveryLog;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.NotificationDeliveryLogRepository;
import com.aiops.monitor.service.CurrentUserService;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentCenterController {

    private final IncidentLogRepository incidentLogRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final CurrentUserService currentUserService;
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
        if ("ACKNOWLEDGED".equals(status)) {
            incident.setAcknowledgedAt(LocalDateTime.now());
        }
        if ("RESOLVED".equals(status)) {
            if (incident.getAcknowledgedAt() == null) {
                incident.setAcknowledgedAt(LocalDateTime.now());
            }
            incident.setResolvedAt(LocalDateTime.now());
        }
        if ("OPEN".equals(status)) {
            incident.setAcknowledgedAt(null);
            incident.setResolvedAt(null);
        }

        incidentLogRepository.save(incident);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", incident.getId());
        response.put("status", incident.getStatus());
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
}
