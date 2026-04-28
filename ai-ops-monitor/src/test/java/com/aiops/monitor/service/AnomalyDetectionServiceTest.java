package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AnomalyResult;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.AnomalyResultRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private SystemMetricsRepository systemMetricsRepository;
    @Mock
    private AnomalyResultRepository anomalyResultRepository;
    @Mock
    private MetricsPublisher metricsPublisher;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    @Test
    void shouldDetectCpuAnomalyAndPublishEvent() {
        SystemMetricsHistory sample = new SystemMetricsHistory();
        sample.setId(1001L);
        sample.setUserId(10L);
        sample.setTargetId(20L);
        sample.setHostname("host-a");
        sample.setCpuUsage(95.0d);

        List<SystemMetricsHistory> history = new ArrayList<>();
        SystemMetricsHistory latest = new SystemMetricsHistory();
        latest.setCpuUsage(95.0d);
        history.add(latest);
        for (int i = 0; i < 30; i++) {
            SystemMetricsHistory row = new SystemMetricsHistory();
            row.setCpuUsage(10.0d);
            history.add(row);
        }

        when(systemMetricsRepository.findTop80ByUserIdAndTargetIdOrderByTimestampDesc(10L, 20L)).thenReturn(history);
        when(anomalyResultRepository.findFirstByUserIdAndTargetIdAndMetricKeyAndStatusOrderByDetectedAtDesc(
                10L, 20L, "CPU", "OPEN")).thenReturn(Optional.empty());
        when(anomalyResultRepository.save(any(AnomalyResult.class))).thenAnswer(invocation -> {
            AnomalyResult result = invocation.getArgument(0);
            if (result.getId() == null) {
                result.setId(2001L);
            }
            return result;
        });

        List<AnomalyResult> results = anomalyDetectionService.detectAndPersist(sample);

        assertFalse(results.isEmpty());
        AnomalyResult anomaly = results.get(0);
        assertEquals("CPU", anomaly.getMetricKey());
        assertEquals("CRITICAL", anomaly.getSeverity());
        assertEquals("OPEN", anomaly.getStatus());
        assertTrue(anomaly.getScore() > 5.0d);
        verify(metricsPublisher, times(1)).send(eq("/topic/anomalies"), any(Map.class));
    }

    @Test
    void shouldBuildAnomalySummaryBySeverity() {
        when(anomalyResultRepository.countByUserIdAndDetectedAtAfter(eq(99L), any(LocalDateTime.class))).thenReturn(12L);
        when(anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(eq(99L), eq("CRITICAL"), any(LocalDateTime.class))).thenReturn(2L);
        when(anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(eq(99L), eq("HIGH"), any(LocalDateTime.class))).thenReturn(3L);
        when(anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(eq(99L), eq("MEDIUM"), any(LocalDateTime.class))).thenReturn(5L);
        when(anomalyResultRepository.countByUserIdAndSeverityAndDetectedAtAfter(eq(99L), eq("LOW"), any(LocalDateTime.class))).thenReturn(2L);

        Map<String, Object> summary = anomalyDetectionService.buildSummary(99L, 30);

        assertEquals(30, summary.get("windowDays"));
        assertEquals(12L, summary.get("totalAnomalies"));
        assertEquals(2L, summary.get("critical"));
        assertEquals(3L, summary.get("high"));
        assertEquals(5L, summary.get("medium"));
        assertEquals(2L, summary.get("low"));
        verify(anomalyResultRepository).countByUserIdAndDetectedAtAfter(eq(99L), any(LocalDateTime.class));
        verify(anomalyResultRepository, times(4))
                .countByUserIdAndSeverityAndDetectedAtAfter(eq(99L), any(String.class), any(LocalDateTime.class));
    }
}
