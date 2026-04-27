package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AgentHeartbeatRequest;
import com.aiops.monitor.model.dto.AgentRegisterRequest;
import com.aiops.monitor.model.dto.MetricDTO;
import com.aiops.monitor.model.entity.AlertEscalationPolicy;
import com.aiops.monitor.model.entity.AlertThresholdConfig;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.EscalationPolicyService;
import com.aiops.monitor.service.InvestigationOrchestrator;
import com.aiops.monitor.service.MetricsPublisher;
import com.aiops.monitor.service.NotificationDispatcherService;
import com.aiops.monitor.service.ThresholdConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentIngestController {

    private final MonitorTargetRepository monitorTargetRepository;
    private final SystemMetricsRepository systemMetricsRepository;
    private final IncidentLogRepository incidentLogRepository;
    private final MetricsPublisher metricsPublisher;
    private final ThresholdConfigService thresholdConfigService;
    private final EscalationPolicyService escalationPolicyService;
    private final NotificationDispatcherService notificationDispatcherService;
    private final InvestigationOrchestrator investigationOrchestrator;
    private final Map<String, LocalDateTime> lastAlertAt = new ConcurrentHashMap<>();
    private final Map<String, Integer> breachCounter = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AgentRegisterRequest request) {
        MonitorTarget target = findTargetByKey(request.getAgentKey());
        ensureTargetEnabled(target);

        target.setHostname(request.getHostname());
        target.setIpAddress(blankToNull(request.getIp()));
        target.setAgentVersion(blankToNull(request.getAgentVersion()));
        target.setLastHeartbeatAt(LocalDateTime.now());
        target.setStatus("ONLINE");
        monitorTargetRepository.save(target);

        return ResponseEntity.ok(Map.of(
                "targetId", target.getId(),
                "name", target.getName(),
                "status", target.getStatus(),
                "serverTime", LocalDateTime.now(),
                "message", "Agent 注册成功"
        ));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@Valid @RequestBody AgentHeartbeatRequest request) {
        MonitorTarget target = findTargetByKey(request.getAgentKey());
        ensureTargetEnabled(target);

        target.setHostname(request.getHostname());
        target.setIpAddress(blankToNull(request.getIp()));
        target.setAgentVersion(blankToNull(request.getAgentVersion()));
        target.setLastHeartbeatAt(LocalDateTime.now());
        target.setStatus("ONLINE");
        monitorTargetRepository.save(target);

        LocalDateTime pointTime = request.getTimestamp() == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getTimestamp()), ZoneId.systemDefault());

        SystemMetricsHistory history = new SystemMetricsHistory();
        history.setCpuUsage(request.getCpuUsage());
        history.setMemUsage(request.getMemUsage());
        history.setDiskUsage(request.getDiskUsage());
        history.setNetRxBytesPerSec(request.getNetRxBytesPerSec());
        history.setNetTxBytesPerSec(request.getNetTxBytesPerSec());
        history.setProcessCount(request.getProcessCount());
        history.setTimestamp(pointTime);
        history.setHostname(request.getHostname());
        history.setUserId(target.getUserId());
        history.setTargetId(target.getId());
        systemMetricsRepository.save(history);

        sendMetric("CPU", request.getCpuUsage(), request, target);
        sendMetric("MEMORY", request.getMemUsage(), request, target);
        sendMetric("DISK", request.getDiskUsage(), request, target);
        sendMetric("NET_RX", request.getNetRxBytesPerSec(), request, target);
        sendMetric("NET_TX", request.getNetTxBytesPerSec(), request, target);
        sendMetric("PROCESS_COUNT", request.getProcessCount() == null ? null : request.getProcessCount().doubleValue(), request, target);

        AlertThresholdConfig threshold = thresholdConfigService.getOrCreateByUserId(target.getUserId());
        AlertEscalationPolicy escalationPolicy = escalationPolicyService.getOrCreateByUserId(target.getUserId());
        evaluateAndRecordIncident(target, "CPU", request.getCpuUsage(), threshold.getCpuThreshold(),
                threshold.getConsecutiveBreachCount(), threshold.getSilenceSeconds(), escalationPolicy);
        evaluateAndRecordIncident(target, "MEMORY", request.getMemUsage(), threshold.getMemoryThreshold(),
                threshold.getConsecutiveBreachCount(), threshold.getSilenceSeconds(), escalationPolicy);
        evaluateAndRecordIncident(target, "DISK", request.getDiskUsage(), threshold.getDiskThreshold(),
                threshold.getConsecutiveBreachCount(), threshold.getSilenceSeconds(), escalationPolicy);
        evaluateAndRecordIncident(target, "PROCESS_COUNT",
                request.getProcessCount() == null ? null : request.getProcessCount().doubleValue(),
                threshold.getProcessCountThreshold(),
                threshold.getConsecutiveBreachCount(),
                threshold.getSilenceSeconds(),
                escalationPolicy);

        return ResponseEntity.ok(Map.of(
                "targetId", target.getId(),
                "accepted", true,
                "serverTime", LocalDateTime.now()
        ));
    }

    private MonitorTarget findTargetByKey(String agentKey) {
        return monitorTargetRepository.findByAgentKey(agentKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "agentKey 无效"));
    }

    private void ensureTargetEnabled(MonitorTarget target) {
        if (!target.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "监控目标已禁用");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void sendMetric(String name, Double value, AgentHeartbeatRequest request, MonitorTarget target) {
        if (value == null) {
            return;
        }
        MetricDTO metric = MetricDTO.builder()
                .name(name)
                .value(value)
                .ip(target.getIpAddress() == null ? "" : target.getIpAddress())
                .hostname(request.getHostname())
                .timestamp(System.currentTimeMillis())
                .build();
        metricsPublisher.send("/topic/metrics", metric);
    }

    private void evaluateAndRecordIncident(MonitorTarget target,
                                           String metricName,
                                           Double value,
                                           double threshold,
                                           Integer consecutiveBreachCount,
                                           Integer silenceSeconds,
                                           AlertEscalationPolicy escalationPolicy) {
        String alertKey = target.getId() + ":" + metricName;
        if (value == null || value <= threshold) {
            breachCounter.remove(alertKey);
            return;
        }

        int requiredCount = consecutiveBreachCount == null || consecutiveBreachCount < 1 ? 1 : consecutiveBreachCount;
        int currentCount = breachCounter.merge(alertKey, 1, Integer::sum);
        if (currentCount < requiredCount) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int effectiveSilenceSeconds = silenceSeconds == null || silenceSeconds < 1 ? 180 : silenceSeconds;
        LocalDateTime last = lastAlertAt.get(alertKey);
        if (last != null && last.plusSeconds(effectiveSilenceSeconds).isAfter(now)) {
            return;
        }

        breachCounter.put(alertKey, 0);
        lastAlertAt.put(alertKey, now);
        String severity = calculateSeverity(value, threshold);
        Integer firstIntervalMinutes = escalationPolicyService.getIntervalMinutes(escalationPolicy, severity, 0);

        IncidentLog incidentEntity = new IncidentLog();
        incidentEntity.setMetricName(metricName);
        incidentEntity.setMetricValue(value);
        incidentEntity.setThreshold(threshold);
        incidentEntity.setMessage(String.format("[%s] %s(%s) %s 连续%d次超阈值: %.2f > %.2f",
                severity,
                target.getName(),
                target.getHostname() == null ? "unknown" : target.getHostname(),
                metricName,
                requiredCount,
                value,
                threshold));
        incidentEntity.setHostname(target.getHostname());
        incidentEntity.setUserId(target.getUserId());
        incidentEntity.setTargetId(target.getId());
        incidentEntity.setSeverity(severity);
        incidentEntity.setStatus("OPEN");
        incidentEntity.setEscalationLevel(0);
        incidentEntity.setLastNotifiedAt(now);
        incidentEntity.setNextNotifyAt(firstIntervalMinutes == null ? null : now.plusMinutes(firstIntervalMinutes));
        incidentEntity.setCreatedAt(now);
        IncidentLog saved = incidentLogRepository.save(incidentEntity);
        notificationDispatcherService.dispatchIncidentOpened(saved);
        try {
            investigationOrchestrator.openFromIncident(saved);
        } catch (Exception ex) {
            log.warn("create investigation failed, incidentId={}, reason={}", saved.getId(), ex.getMessage());
        }
    }

    private String calculateSeverity(double value, double threshold) {
        if (threshold <= 0) {
            return "P2";
        }
        double ratio = value / threshold;
        if (ratio >= 1.5d) {
            return "P1";
        }
        if (ratio >= 1.2d) {
            return "P2";
        }
        return "P3";
    }
}
