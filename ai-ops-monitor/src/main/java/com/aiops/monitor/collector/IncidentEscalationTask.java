package com.aiops.monitor.collector;

import com.aiops.monitor.model.entity.AlertEscalationPolicy;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.service.EscalationPolicyService;
import com.aiops.monitor.service.NotificationDispatcherService;
import com.aiops.monitor.service.TargetNotificationSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentEscalationTask {

    private final IncidentLogRepository incidentLogRepository;
    private final EscalationPolicyService escalationPolicyService;
    private final NotificationDispatcherService notificationDispatcherService;
    private final TargetNotificationSubscriptionService targetNotificationSubscriptionService;

    @Scheduled(fixedDelayString = "${monitor.escalation.scan-ms:30000}")
    public void processEscalations() {
        LocalDateTime now = LocalDateTime.now();
        PageRequest pageable = PageRequest.of(0, 100, Sort.by("nextNotifyAt").ascending());
        Page<IncidentLog> incidents = incidentLogRepository.findEscalationCandidates(now, pageable);

        if (incidents.isEmpty()) {
            return;
        }

        for (IncidentLog incident : incidents.getContent()) {
            try {
                escalateIncident(incident, now);
            } catch (Exception ex) {
                log.warn("告警升级处理失败, incidentId={}", incident.getId(), ex);
            }
        }
    }

    private void escalateIncident(IncidentLog incident, LocalDateTime now) {
        if (incident.getTargetId() == null) {
            return;
        }
        AlertEscalationPolicy policy = escalationPolicyService.resolvePolicyForTarget(incident.getTargetId());
        if (policy == null || targetNotificationSubscriptionService.countSubscribers(incident.getTargetId()) <= 0) {
            incident.setNextNotifyAt(null);
            incidentLogRepository.save(incident);
            return;
        }

        int currentLevel = incident.getEscalationLevel() == null ? 0 : incident.getEscalationLevel();
        int newLevel = currentLevel + 1;
        String severity = normalizeSeverity(incident.getSeverity());

        Integer nextIntervalMinutes = escalationPolicyService.getIntervalMinutes(policy, severity, newLevel);
        incident.setEscalationLevel(newLevel);
        incident.setLastNotifiedAt(now);
        incident.setNextNotifyAt(nextIntervalMinutes == null ? null : now.plusMinutes(nextIntervalMinutes));
        incidentLogRepository.save(incident);

        notificationDispatcherService.dispatchIncidentEscalated(incident);
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.trim().isEmpty()) {
            return "P2";
        }
        String value = severity.trim().toUpperCase(Locale.ROOT);
        if (!"P1".equals(value) && !"P2".equals(value) && !"P3".equals(value)) {
            return "P2";
        }
        return value;
    }
}
