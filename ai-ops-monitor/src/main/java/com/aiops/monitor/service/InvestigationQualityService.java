package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AiActionRun;
import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.repository.AiActionRunRepository;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.IncidentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvestigationQualityService {

    private static final int WINDOW_DAYS = 30;

    private final AiInvestigationRepository aiInvestigationRepository;
    private final AiActionRunRepository aiActionRunRepository;
    private final IncidentLogRepository incidentLogRepository;

    public Map<String, Object> buildSummary(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now().minusDays(WINDOW_DAYS);

        long openInvestigations = aiInvestigationRepository.countByUserIdAndStatusNot(userId, "CLOSED");
        List<AiInvestigation> closedInvestigations = aiInvestigationRepository
                .findByUserIdAndClosedAtIsNotNullAndClosedAtAfter(userId, windowStart);

        double avgInvestigationMins = averageInvestigationMinutes(closedInvestigations);
        long closedWithoutExecution = closedInvestigations.stream()
                .filter(inv -> aiActionRunRepository.countByInvestigationIdAndUserId(inv.getId(), userId) == 0L)
                .count();
        double falsePositiveRate = closedInvestigations.isEmpty()
                ? 0d
                : (closedWithoutExecution * 100d / closedInvestigations.size());

        List<AiActionRun> runs = aiActionRunRepository.findByUserIdAndCreatedAtAfter(userId, windowStart);
        long successRuns = runs.stream().filter(run -> "SUCCESS".equalsIgnoreCase(run.getStatus())).count();
        double actionRunSuccessRate = runs.isEmpty() ? 0d : (successRuns * 100d / runs.size());

        long totalIncidents = incidentLogRepository.countByUserIdAndCreatedAtAfter(userId, windowStart);
        long resolvedIncidentsCount = incidentLogRepository
                .countByUserIdAndCreatedAtAfterAndStatus(userId, windowStart, "RESOLVED");
        List<IncidentLog> resolvedIncidents = incidentLogRepository
                .findByUserIdAndResolvedAtIsNotNullAndCreatedAtAfter(userId, windowStart);
        double mttrMinutes = resolvedIncidents.isEmpty()
                ? 0d
                : resolvedIncidents.stream()
                .mapToDouble(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes())
                .average()
                .orElse(0d);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("windowDays", WINDOW_DAYS);
        summary.put("openInvestigations", openInvestigations);
        summary.put("closedInvestigations", closedInvestigations.size());
        summary.put("avgInvestigationMinutes", round(avgInvestigationMins));
        summary.put("falsePositiveRate", round(falsePositiveRate));
        summary.put("actionRunTotal", runs.size());
        summary.put("actionRunSuccess", successRuns);
        summary.put("actionRunSuccessRate", round(actionRunSuccessRate));
        summary.put("totalIncidents", totalIncidents);
        summary.put("resolvedIncidents", resolvedIncidentsCount);
        summary.put("mttrMinutes", round(mttrMinutes));
        return summary;
    }

    private double averageInvestigationMinutes(List<AiInvestigation> closedInvestigations) {
        if (closedInvestigations.isEmpty()) {
            return 0d;
        }
        return closedInvestigations.stream()
                .filter(inv -> inv.getStartedAt() != null && inv.getClosedAt() != null)
                .mapToDouble(inv -> Duration.between(inv.getStartedAt(), inv.getClosedAt()).toMinutes())
                .average()
                .orElse(0d);
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
