package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AgentLogBatchRequest;
import com.aiops.monitor.model.dto.AgentLogItem;
import com.aiops.monitor.model.dto.AgentTraceBatchRequest;
import com.aiops.monitor.model.dto.AgentTraceSpanItem;
import com.aiops.monitor.model.entity.LogRecord;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.TraceSpan;
import com.aiops.monitor.repository.LogRecordRepository;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.TraceSpanRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class ObservabilityIngestController {

    private final MonitorTargetRepository monitorTargetRepository;
    private final LogRecordRepository logRecordRepository;
    private final TraceSpanRepository traceSpanRepository;

    @PostMapping("/logs")
    public ResponseEntity<Map<String, Object>> ingestLogs(@Valid @RequestBody AgentLogBatchRequest request) {
        MonitorTarget target = findTargetByKey(request.getAgentKey());
        ensureTargetEnabled(target);

        LocalDateTime now = LocalDateTime.now();
        List<LogRecord> batch = new ArrayList<>();
        String effectiveHost = notBlank(request.getHostname()) ? request.getHostname() : target.getHostname();
        String effectiveSource = notBlank(request.getSource()) ? request.getSource() : "agent";

        for (AgentLogItem item : request.getLogs()) {
            LogRecord record = new LogRecord();
            record.setUserId(null);
            record.setTargetId(target.getId());
            record.setHostname(effectiveHost);
            record.setServiceName(notBlank(request.getServiceName()) ? request.getServiceName() : target.getName());
            record.setSource(effectiveSource);
            record.setLevel(item.getLevel().trim().toUpperCase());
            record.setMessage(item.getMessage());
            record.setTraceId(blankToNull(item.getTraceId()));
            record.setLabelsJson(blankToNull(item.getLabelsJson()));
            record.setOccurredAt(resolveTime(item.getTimestamp(), now));
            record.setCreatedAt(now);
            batch.add(record);
        }

        logRecordRepository.saveAll(batch);
        return ResponseEntity.ok(Map.of(
                "accepted", true,
                "targetId", target.getId(),
                "count", batch.size(),
                "serverTime", now
        ));
    }

    @PostMapping("/traces")
    public ResponseEntity<Map<String, Object>> ingestTraces(@Valid @RequestBody AgentTraceBatchRequest request) {
        MonitorTarget target = findTargetByKey(request.getAgentKey());
        ensureTargetEnabled(target);

        LocalDateTime now = LocalDateTime.now();
        List<TraceSpan> batch = new ArrayList<>();
        String effectiveHost = notBlank(request.getHostname()) ? request.getHostname() : target.getHostname();

        for (AgentTraceSpanItem item : request.getTraces()) {
            TraceSpan span = new TraceSpan();
            span.setUserId(null);
            span.setTargetId(target.getId());
            span.setHostname(effectiveHost);
            span.setServiceName(notBlank(request.getServiceName()) ? request.getServiceName() : target.getName());
            span.setTraceId(item.getTraceId().trim());
            span.setSpanId(blankToNull(item.getSpanId()));
            span.setParentSpanId(blankToNull(item.getParentSpanId()));
            span.setOperationName(blankToNull(item.getOperationName()));
            span.setDurationMs(item.getDurationMs());
            span.setStatus(blankToNull(item.getStatus()));
            span.setAttributesJson(blankToNull(item.getAttributesJson()));
            span.setStartedAt(resolveTime(item.getTimestamp(), now));
            span.setCreatedAt(now);
            batch.add(span);
        }

        traceSpanRepository.saveAll(batch);
        return ResponseEntity.ok(Map.of(
                "accepted", true,
                "targetId", target.getId(),
                "count", batch.size(),
                "serverTime", now
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

    private LocalDateTime resolveTime(Long epochMs, LocalDateTime fallback) {
        if (epochMs == null || epochMs <= 0) {
            return fallback;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
    }

    private String blankToNull(String value) {
        if (!notBlank(value)) return null;
        return value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
