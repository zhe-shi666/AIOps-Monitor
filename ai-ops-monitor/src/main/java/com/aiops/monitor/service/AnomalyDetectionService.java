package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AnomalyResult;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.AnomalyResultRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final int MIN_BASELINE_POINTS = 20;
    private static final int COOLDOWN_SECONDS = 180;

    private final SystemMetricsRepository systemMetricsRepository;
    private final AnomalyResultRepository anomalyResultRepository;
    private final MetricsPublisher metricsPublisher;

    public List<AnomalyResult> detectAndPersist(SystemMetricsHistory sample) {
        if (sample == null || sample.getUserId() == null || sample.getTargetId() == null) {
            return List.of();
        }
        List<SystemMetricsHistory> history = systemMetricsRepository
                .findTop80ByUserIdAndTargetIdOrderByTimestampDesc(sample.getUserId(), sample.getTargetId());
        if (history.size() < MIN_BASELINE_POINTS) {
            return List.of();
        }

        List<AnomalyResult> results = new ArrayList<>();
        results.addAll(detectMetric(sample, history, "CPU", sample.getCpuUsage(), 2.8d));
        results.addAll(detectMetric(sample, history, "MEMORY", sample.getMemUsage(), 2.8d));
        results.addAll(detectMetric(sample, history, "DISK", sample.getDiskUsage(), 3.0d));
        results.addAll(detectMetric(sample, history, "NET_RX", sample.getNetRxBytesPerSec(), 3.2d));
        results.addAll(detectMetric(sample, history, "NET_TX", sample.getNetTxBytesPerSec(), 3.2d));
        results.addAll(detectMetric(sample, history, "PROCESS_COUNT",
                sample.getProcessCount() == null ? null : sample.getProcessCount().doubleValue(), 3.0d));
        return results;
    }

    public Map<String, Object> buildSummary(Long userId, int days) {
        int windowDays = Math.max(1, Math.min(365, days));
        LocalDateTime start = LocalDateTime.now().minusDays(windowDays);
        long total = anomalyResultRepository.countByUserIdAndDetectedAtAfter(userId, start);
        long critical = anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(userId, "CRITICAL", start);
        long high = anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(userId, "HIGH", start);
        long medium = anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(userId, "MEDIUM", start);
        long low = anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(userId, "LOW", start);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("windowDays", windowDays);
        result.put("totalAnomalies", total);
        result.put("critical", critical);
        result.put("high", high);
        result.put("medium", medium);
        result.put("low", low);
        return result;
    }

    private List<AnomalyResult> detectMetric(SystemMetricsHistory sample,
                                             List<SystemMetricsHistory> history,
                                             String metricKey,
                                             Double observed,
                                             double zThreshold) {
        if (observed == null) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            Double value = metricValue(history.get(i), metricKey);
            if (value != null) {
                values.add(value);
            }
        }
        if (values.size() < MIN_BASELINE_POINTS) {
            return List.of();
        }

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0d);
        double std = Math.sqrt(Math.max(variance, 1e-9));
        double score = Math.abs(observed - mean) / std;
        if (score < zThreshold) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        AnomalyResult existing = anomalyResultRepository
                .findFirstByUserIdAndTargetIdAndMetricKeyAndStatusOrderByDetectedAtDesc(
                        sample.getUserId(),
                        sample.getTargetId(),
                        metricKey,
                        "OPEN"
                ).orElse(null);

        if (existing != null && existing.getDetectedAt() != null
                && existing.getDetectedAt().plusSeconds(COOLDOWN_SECONDS).isAfter(now)) {
            existing.setObserved(observed);
            existing.setBaseline(mean);
            existing.setScore(score);
            existing.setDetectedAt(now);
            existing.setSeverity(resolveSeverity(score));
            anomalyResultRepository.save(existing);
            publishAnomaly(existing);
            return List.of(existing);
        }

        AnomalyResult anomaly = new AnomalyResult();
        anomaly.setUserId(sample.getUserId());
        anomaly.setTargetId(sample.getTargetId());
        anomaly.setHostname(sample.getHostname());
        anomaly.setMetricKey(metricKey);
        anomaly.setObserved(observed);
        anomaly.setBaseline(mean);
        anomaly.setScore(score);
        anomaly.setSeverity(resolveSeverity(score));
        anomaly.setStatus("OPEN");
        anomaly.setDetectedAt(now);
        anomaly.setSourceMetricId(sample.getId());
        anomaly.setCreatedAt(now);
        AnomalyResult saved = anomalyResultRepository.save(anomaly);
        publishAnomaly(saved);
        return List.of(saved);
    }

    private void publishAnomaly(AnomalyResult anomaly) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", anomaly.getId());
        payload.put("targetId", anomaly.getTargetId());
        payload.put("hostname", anomaly.getHostname());
        payload.put("metricKey", anomaly.getMetricKey());
        payload.put("severity", anomaly.getSeverity());
        payload.put("score", round(anomaly.getScore()));
        payload.put("baseline", anomaly.getBaseline());
        payload.put("observed", anomaly.getObserved());
        payload.put("detectedAt", anomaly.getDetectedAt());
        metricsPublisher.send("/topic/anomalies", payload);
    }

    private Double metricValue(SystemMetricsHistory row, String metricKey) {
        return switch (metricKey.toUpperCase(Locale.ROOT)) {
            case "CPU" -> row.getCpuUsage();
            case "MEMORY" -> row.getMemUsage();
            case "DISK" -> row.getDiskUsage();
            case "NET_RX" -> row.getNetRxBytesPerSec();
            case "NET_TX" -> row.getNetTxBytesPerSec();
            case "PROCESS_COUNT" -> row.getProcessCount() == null ? null : row.getProcessCount().doubleValue();
            default -> null;
        };
    }

    private String resolveSeverity(double score) {
        if (score >= 5d) return "CRITICAL";
        if (score >= 4d) return "HIGH";
        if (score >= 3d) return "MEDIUM";
        return "LOW";
    }

    private double round(Double value) {
        if (value == null) return 0d;
        return Math.round(value * 100d) / 100d;
    }
}
