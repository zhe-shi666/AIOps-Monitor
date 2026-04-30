package com.aiops.monitor.collector;

import com.aiops.monitor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataRetentionTask {
    private final SystemMetricsRepository systemMetricsRepository;
    private final LogRecordRepository logRecordRepository;
    private final TraceSpanRepository traceSpanRepository;
    private final IncidentLogRepository incidentLogRepository;
    private final AnomalyResultRepository anomalyResultRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${monitor.retention.metrics-days:30}")
    private int metricsDays;

    @Value("${monitor.retention.logs-days:14}")
    private int logsDays;

    @Value("${monitor.retention.traces-days:14}")
    private int tracesDays;

    @Value("${monitor.retention.resolved-incidents-days:180}")
    private int resolvedIncidentsDays;

    @Value("${monitor.retention.closed-anomalies-days:90}")
    private int closedAnomaliesDays;

    @Value("${monitor.retention.audit-days:365}")
    private int auditDays;

    @Scheduled(cron = "${monitor.retention.cron:0 30 3 * * *}")
    @Transactional
    public void cleanup() {
        int metrics = systemMetricsRepository.deleteOlderThan(LocalDateTime.now().minusDays(metricsDays));
        int logs = logRecordRepository.deleteOlderThan(LocalDateTime.now().minusDays(logsDays));
        int traces = traceSpanRepository.deleteOlderThan(LocalDateTime.now().minusDays(tracesDays));
        int incidents = incidentLogRepository.deleteResolvedOlderThan(LocalDateTime.now().minusDays(resolvedIncidentsDays));
        int anomalies = anomalyResultRepository.deleteClosedOlderThan(LocalDateTime.now().minusDays(closedAnomaliesDays));
        int audits = auditLogRepository.deleteOlderThan(LocalDateTime.now().minusDays(auditDays));
        log.info("retention cleanup completed, metrics={}, logs={}, traces={}, incidents={}, anomalies={}, audits={}",
                metrics, logs, traces, incidents, anomalies, audits);
    }
}
