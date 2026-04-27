package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AgentHeartbeatRequest;
import com.aiops.monitor.model.dto.AgentRegisterRequest;
import com.aiops.monitor.model.dto.MetricDTO;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.MetricsPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
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
public class AgentIngestController {

    private final MonitorTargetRepository monitorTargetRepository;
    private final SystemMetricsRepository systemMetricsRepository;
    private final IncidentLogRepository incidentLogRepository;
    private final MetricsPublisher metricsPublisher;
    private final Map<String, LocalDateTime> lastAlertAt = new ConcurrentHashMap<>();

    @Value("${monitor.threshold.cpu:70}")
    private double cpuThreshold;

    @Value("${monitor.threshold.memory:90}")
    private double memoryThreshold;

    @Value("${monitor.threshold.disk:85}")
    private double diskThreshold;

    @Value("${monitor.threshold.process-count:400}")
    private int processCountThreshold;

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

        evaluateAndRecordIncident(target, "CPU", request.getCpuUsage(), cpuThreshold);
        evaluateAndRecordIncident(target, "MEMORY", request.getMemUsage(), memoryThreshold);
        evaluateAndRecordIncident(target, "DISK", request.getDiskUsage(), diskThreshold);
        evaluateAndRecordIncident(target, "PROCESS_COUNT",
                request.getProcessCount() == null ? null : request.getProcessCount().doubleValue(),
                processCountThreshold);

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

    private void evaluateAndRecordIncident(MonitorTarget target, String metricName, Double value, double threshold) {
        if (value == null || value <= threshold) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String alertKey = target.getId() + ":" + metricName;
        LocalDateTime last = lastAlertAt.get(alertKey);
        if (last != null && last.plusMinutes(3).isAfter(now)) {
            return;
        }
        lastAlertAt.put(alertKey, now);

        IncidentLog log = new IncidentLog();
        log.setMetricName(metricName);
        log.setMetricValue(value);
        log.setThreshold(threshold);
        log.setMessage(String.format("%s(%s) %s 超阈值: %.2f > %.2f",
                target.getName(),
                target.getHostname() == null ? "unknown" : target.getHostname(),
                metricName,
                value,
                threshold));
        log.setHostname(target.getHostname());
        log.setUserId(target.getUserId());
        log.setTargetId(target.getId());
        log.setCreatedAt(now);
        incidentLogRepository.save(log);
    }
}
