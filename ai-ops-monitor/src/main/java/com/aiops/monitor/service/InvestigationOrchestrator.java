package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.AiObservation;
import com.aiops.monitor.model.entity.AiReportSnapshot;
import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.AiObservationRepository;
import com.aiops.monitor.repository.AiReportSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationOrchestrator {

    private final AiInvestigationRepository aiInvestigationRepository;
    private final AiObservationRepository aiObservationRepository;
    private final AiReportSnapshotRepository aiReportSnapshotRepository;
    private final InvestigationIntelligenceService investigationIntelligenceService;
    private final InvestigationEventPublisher eventPublisher;

    @Transactional
    public AiInvestigation openFromIncident(IncidentLog incident) {
        if (incident == null || incident.getUserId() == null || incident.getId() == null) {
            return null;
        }

        AiInvestigation investigation = new AiInvestigation();
        investigation.setUserId(incident.getUserId());
        investigation.setIncidentId(incident.getId());
        investigation.setTargetId(incident.getTargetId());
        investigation.setTriggerSource("INCIDENT");
        investigation.setStatus("COLLECTING");
        investigation.setSeverity(normalizeSeverity(incident.getSeverity()));
        investigation.setTitle(buildTitle(incident));
        investigation.setSummary(incident.getMessage());
        investigation.setStartedAt(LocalDateTime.now());
        investigation.setCreatedAt(LocalDateTime.now());
        investigation.setUpdatedAt(LocalDateTime.now());
        AiInvestigation saved = aiInvestigationRepository.save(investigation);

        AiObservation observation = new AiObservation();
        observation.setInvestigationId(saved.getId());
        observation.setUserId(saved.getUserId());
        observation.setType("METRIC");
        observation.setSourceRef("incident_log:" + incident.getId());
        observation.setHostname(incident.getHostname());
        observation.setMetricName(incident.getMetricName());
        observation.setMetricValue(incident.getMetricValue());
        observation.setObservedAt(incident.getCreatedAt() == null ? LocalDateTime.now() : incident.getCreatedAt());
        observation.setConfidence(0.9d);
        observation.setPayloadJson(buildObservationPayload(incident));
        aiObservationRepository.save(observation);

        AiReportSnapshot snapshot = new AiReportSnapshot();
        snapshot.setInvestigationId(saved.getId());
        snapshot.setUserId(saved.getUserId());
        snapshot.setVersionNo(1);
        snapshot.setFormat("MARKDOWN");
        snapshot.setCreatedBy("SYSTEM");
        snapshot.setReportMarkdown(buildInitialReportMarkdown(saved, incident));
        snapshot.setCreatedAt(LocalDateTime.now());
        aiReportSnapshotRepository.save(snapshot);

        eventPublisher.publish(
                saved.getUserId(),
                "INVESTIGATION_CREATED",
                saved.getId(),
                "Investigation opened from incident",
                java.util.Map.of(
                        "incidentId", incident.getId(),
                        "severity", saved.getSeverity(),
                        "metric", incident.getMetricName()
                )
        );
        investigationIntelligenceService.autoGenerateFromIncident(saved.getId(), saved.getUserId());

        log.info("🧭 Investigation 已创建: id={}, incidentId={}", saved.getId(), incident.getId());
        return saved;
    }

    private String buildTitle(IncidentLog incident) {
        String metricName = incident.getMetricName() == null ? "METRIC" : incident.getMetricName();
        String host = incident.getHostname() == null ? "unknown-host" : incident.getHostname();
        return "[" + normalizeSeverity(incident.getSeverity()) + "] " + metricName + " @ " + host;
    }

    private String normalizeSeverity(String severity) {
        if ("P1".equalsIgnoreCase(severity)) return "P1";
        if ("P3".equalsIgnoreCase(severity)) return "P3";
        return "P2";
    }

    private String buildObservationPayload(IncidentLog incident) {
        String message = incident.getMessage() == null ? "" : incident.getMessage().replace("\"", "\\\"");
        return String.format(
                "{\"threshold\":%s,\"severity\":\"%s\",\"message\":\"%s\"}",
                incident.getThreshold(),
                normalizeSeverity(incident.getSeverity()),
                message
        );
    }

    private String buildInitialReportMarkdown(AiInvestigation investigation, IncidentLog incident) {
        return String.format("""
                ## Investigation #%d
                - 状态: %s
                - 来源事件: #%d
                - 指标: %s
                - 当前值/阈值: %.2f / %.2f
                - 节点: %s

                系统已自动创建调查对象，下一步将补充证据并生成结构化根因假设。
                """,
                investigation.getId(),
                investigation.getStatus(),
                incident.getId(),
                incident.getMetricName(),
                incident.getMetricValue(),
                incident.getThreshold(),
                incident.getHostname() == null ? "unknown-host" : incident.getHostname()
        );
    }
}
