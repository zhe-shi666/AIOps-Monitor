package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AgentHeartbeatRequest;
import com.aiops.monitor.model.dto.AgentRegisterRequest;
import com.aiops.monitor.model.dto.MetricDTO;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.MetricsPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentIngestController {

    private final MonitorTargetRepository monitorTargetRepository;
    private final SystemMetricsRepository systemMetricsRepository;
    private final MetricsPublisher metricsPublisher;

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
        history.setTimestamp(pointTime);
        history.setHostname(request.getHostname());
        history.setUserId(target.getUserId());
        history.setTargetId(target.getId());
        systemMetricsRepository.save(history);

        MetricDTO cpuMetric = MetricDTO.builder()
                .name("CPU")
                .value(request.getCpuUsage())
                .ip(target.getIpAddress() == null ? "" : target.getIpAddress())
                .hostname(request.getHostname())
                .timestamp(System.currentTimeMillis())
                .build();
        metricsPublisher.send("/topic/metrics", cpuMetric);

        MetricDTO memMetric = MetricDTO.builder()
                .name("MEMORY")
                .value(request.getMemUsage())
                .ip(target.getIpAddress() == null ? "" : target.getIpAddress())
                .hostname(request.getHostname())
                .timestamp(System.currentTimeMillis())
                .build();
        metricsPublisher.send("/topic/metrics", memMetric);

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
}
