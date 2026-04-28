package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AiActionRun;
import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.repository.AiActionRunRepository;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.IncidentLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestigationQualityServiceTest {

    @Mock
    private AiInvestigationRepository aiInvestigationRepository;
    @Mock
    private AiActionRunRepository aiActionRunRepository;
    @Mock
    private IncidentLogRepository incidentLogRepository;

    @InjectMocks
    private InvestigationQualityService investigationQualityService;

    @Test
    void shouldBuildQualitySummaryWithCalculatedRates() {
        Long userId = 7L;
        AiInvestigation inv1 = investigationWithDuration(1L, 10);
        AiInvestigation inv2 = investigationWithDuration(2L, 20);

        when(aiInvestigationRepository.countByUserIdAndStatusNot(userId, "CLOSED")).thenReturn(3L);
        when(aiInvestigationRepository.findByUserIdAndClosedAtIsNotNullAndClosedAtAfter(eq(userId), any()))
                .thenReturn(List.of(inv1, inv2));
        when(aiActionRunRepository.countByInvestigationIdAndUserId(1L, userId)).thenReturn(0L);
        when(aiActionRunRepository.countByInvestigationIdAndUserId(2L, userId)).thenReturn(1L);
        when(aiActionRunRepository.findByUserIdAndCreatedAtAfter(eq(userId), any()))
                .thenReturn(List.of(run("SUCCESS"), run("FAILED"), run("SUCCESS")));

        when(incidentLogRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(10L);
        when(incidentLogRepository.countByUserIdAndCreatedAtAfterAndStatus(eq(userId), any(), eq("RESOLVED")))
                .thenReturn(6L);
        when(incidentLogRepository.findByUserIdAndResolvedAtIsNotNullAndCreatedAtAfter(eq(userId), any()))
                .thenReturn(List.of(incidentWithDuration(5), incidentWithDuration(15)));

        Map<String, Object> summary = investigationQualityService.buildSummary(userId);

        assertEquals(3L, summary.get("openInvestigations"));
        assertEquals(2, summary.get("closedInvestigations"));
        assertEquals(15.0d, summary.get("avgInvestigationMinutes"));
        assertEquals(50.0d, summary.get("falsePositiveRate"));
        assertEquals(3, summary.get("actionRunTotal"));
        assertEquals(2L, summary.get("actionRunSuccess"));
        assertEquals(66.67d, summary.get("actionRunSuccessRate"));
        assertEquals(10L, summary.get("totalIncidents"));
        assertEquals(6L, summary.get("resolvedIncidents"));
        assertEquals(10.0d, summary.get("mttrMinutes"));
    }

    @Test
    void shouldHandleEmptyData() {
        Long userId = 8L;
        when(aiInvestigationRepository.countByUserIdAndStatusNot(userId, "CLOSED")).thenReturn(0L);
        when(aiInvestigationRepository.findByUserIdAndClosedAtIsNotNullAndClosedAtAfter(eq(userId), any()))
                .thenReturn(List.of());
        when(aiActionRunRepository.findByUserIdAndCreatedAtAfter(eq(userId), any()))
                .thenReturn(List.of());
        when(incidentLogRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(0L);
        when(incidentLogRepository.countByUserIdAndCreatedAtAfterAndStatus(eq(userId), any(), eq("RESOLVED")))
                .thenReturn(0L);
        when(incidentLogRepository.findByUserIdAndResolvedAtIsNotNullAndCreatedAtAfter(eq(userId), any()))
                .thenReturn(List.of());

        Map<String, Object> summary = investigationQualityService.buildSummary(userId);
        assertEquals(0.0d, summary.get("falsePositiveRate"));
        assertEquals(0.0d, summary.get("actionRunSuccessRate"));
        assertEquals(0.0d, summary.get("mttrMinutes"));
    }

    private AiInvestigation investigationWithDuration(Long id, long minutes) {
        AiInvestigation i = new AiInvestigation();
        i.setId(id);
        i.setStartedAt(LocalDateTime.now().minusMinutes(minutes));
        i.setClosedAt(LocalDateTime.now());
        return i;
    }

    private AiActionRun run(String status) {
        AiActionRun run = new AiActionRun();
        run.setStatus(status);
        return run;
    }

    private IncidentLog incidentWithDuration(long minutes) {
        IncidentLog incident = new IncidentLog();
        incident.setCreatedAt(LocalDateTime.now().minusMinutes(minutes));
        incident.setResolvedAt(LocalDateTime.now());
        return incident;
    }
}
