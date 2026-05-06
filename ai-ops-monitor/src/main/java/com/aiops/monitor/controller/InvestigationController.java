package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AiActionExecuteRequest;
import com.aiops.monitor.model.dto.AiActionRollbackRequest;
import com.aiops.monitor.model.dto.AiActionApproveRequest;
import com.aiops.monitor.model.dto.AiActionPlanCreateRequest;
import com.aiops.monitor.model.dto.AiActionRetryRequest;
import com.aiops.monitor.model.dto.AiHypothesisCreateRequest;
import com.aiops.monitor.model.dto.AiInvestigationCreateRequest;
import com.aiops.monitor.model.dto.AiInvestigationGenerateRequest;
import com.aiops.monitor.model.dto.AiObservationCreateRequest;
import com.aiops.monitor.model.dto.AiPostmortemGenerateRequest;
import com.aiops.monitor.model.dto.AiReportSnapshotCreateRequest;
import com.aiops.monitor.model.entity.AiActionPlan;
import com.aiops.monitor.model.entity.AiActionRun;
import com.aiops.monitor.model.entity.AiActionAudit;
import com.aiops.monitor.model.entity.AiHypothesis;
import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.AiRollbackRun;
import com.aiops.monitor.model.entity.AiObservation;
import com.aiops.monitor.model.entity.AiReportSnapshot;
import com.aiops.monitor.model.entity.AiModelTrace;
import com.aiops.monitor.model.entity.RcaReport;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.AiActionPlanRepository;
import com.aiops.monitor.repository.AiActionRunRepository;
import com.aiops.monitor.repository.AiActionAuditRepository;
import com.aiops.monitor.repository.AiHypothesisRepository;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.AiRollbackRunRepository;
import com.aiops.monitor.repository.AiObservationRepository;
import com.aiops.monitor.repository.AiReportSnapshotRepository;
import com.aiops.monitor.repository.AiModelTraceRepository;
import com.aiops.monitor.repository.RcaReportRepository;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.InvestigationEventPublisher;
import com.aiops.monitor.service.InvestigationIntelligenceService;
import com.aiops.monitor.service.InvestigationQualityService;
import com.aiops.monitor.service.RoleGuardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/investigations")
@RequiredArgsConstructor
public class InvestigationController {

    private final AiInvestigationRepository aiInvestigationRepository;
    private final AiObservationRepository aiObservationRepository;
    private final AiHypothesisRepository aiHypothesisRepository;
    private final AiActionPlanRepository aiActionPlanRepository;
    private final AiActionRunRepository aiActionRunRepository;
    private final AiActionAuditRepository aiActionAuditRepository;
    private final AiRollbackRunRepository aiRollbackRunRepository;
    private final AiReportSnapshotRepository aiReportSnapshotRepository;
    private final RcaReportRepository rcaReportRepository;
    private final AiModelTraceRepository aiModelTraceRepository;
    private final CurrentUserService currentUserService;
    private final InvestigationQualityService investigationQualityService;
    private final InvestigationIntelligenceService investigationIntelligenceService;
    private final InvestigationEventPublisher investigationEventPublisher;
    private final RoleGuardService roleGuardService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<Page<AiInvestigation>> list(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String status,
                                                      Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String normalizedStatus = normalizeUpper(status);
        Page<AiInvestigation> result = normalizedStatus == null
                ? aiInvestigationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : aiInvestigationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), normalizedStatus, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/quality/summary")
    public ResponseEntity<Map<String, Object>> qualitySummary(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(investigationQualityService.buildSummary(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody(required = false) AiInvestigationCreateRequest request,
                                                      Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        AiInvestigation investigation = new AiInvestigation();
        investigation.setUserId(user.getId());
        if (request != null) {
            investigation.setIncidentId(request.getIncidentId());
            investigation.setTargetId(request.getTargetId());
            investigation.setSummary(normalize(request.getSummary()));
            investigation.setTitle(normalize(request.getTitle()));
        }

        String triggerSource = request == null ? null : normalizeUpper(request.getTriggerSource());
        investigation.setTriggerSource(triggerSource == null ? "MANUAL" : triggerSource);
        investigation.setStatus("COLLECTING");
        investigation.setSeverity(resolveSeverity(request == null ? null : request.getSeverity()));
        investigation.setStartedAt(LocalDateTime.now());
        investigation.setUpdatedAt(LocalDateTime.now());
        investigation.setCreatedAt(LocalDateTime.now());

        AiInvestigation saved = aiInvestigationRepository.save(investigation);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("status", saved.getStatus());
        result.put("severity", saved.getSeverity());
        result.put("triggerSource", saved.getTriggerSource());
        result.put("createdAt", saved.getCreatedAt());
        investigationEventPublisher.publish(
                user.getId(),
                "INVESTIGATION_CREATED",
                saved.getId(),
                "Manual investigation created",
                Map.of("status", saved.getStatus(), "severity", saved.getSeverity())
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        AiInvestigation investigation = requireInvestigation(id, user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("investigation", investigation);
        result.put("observations", aiObservationRepository.findByInvestigationIdAndUserIdOrderByObservedAtAsc(id, user.getId()));
        result.put("hypotheses", aiHypothesisRepository.findByInvestigationIdAndUserIdOrderByRankOrderAsc(id, user.getId()));
        result.put("actionPlans", aiActionPlanRepository.findByInvestigationIdAndUserIdOrderByCreatedAtAsc(id, user.getId()));
        result.put("actionRuns", aiActionRunRepository.findByInvestigationIdAndUserIdOrderByCreatedAtAsc(id, user.getId()));
        result.put("rollbackRuns", aiRollbackRunRepository.findByInvestigationIdAndUserIdOrderByCreatedAtDesc(id, user.getId()));
        result.put("latestSnapshot", aiReportSnapshotRepository.findFirstByInvestigationIdAndUserIdOrderByVersionNoDesc(id, user.getId()).orElse(null));
        result.put("snapshots", aiReportSnapshotRepository.findByInvestigationIdAndUserIdOrderByVersionNoDesc(id, user.getId()));
        result.put("latestRcaReport", rcaReportRepository.findFirstByInvestigationIdAndUserIdOrderByCreatedAtDesc(id, user.getId()).orElse(null));
        result.put("recentActionAudits", aiActionAuditRepository.search(id, user.getId(), null, null, PageRequest.of(0, 20)).getContent());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/rca-reports")
    public ResponseEntity<List<RcaReport>> rcaReports(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        requireInvestigation(id, user.getId());
        int limitedSize = Math.max(1, Math.min(50, size));
        List<RcaReport> reports = rcaReportRepository.findByInvestigationIdAndUserIdOrderByCreatedAtDesc(
                id,
                user.getId(),
                PageRequest.of(0, limitedSize)
        );
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}/model-traces")
    public ResponseEntity<Page<AiModelTrace>> modelTraces(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        requireInvestigation(id, user.getId());
        int safeSize = Math.max(1, Math.min(100, size));
        PageRequest pageable = PageRequest.of(Math.max(0, page), safeSize, Sort.by("createdAt").descending());
        Page<AiModelTrace> traces = aiModelTraceRepository.findByInvestigationIdAndUserIdOrderByCreatedAtDesc(id, user.getId(), pageable);
        return ResponseEntity.ok(traces);
    }

    @GetMapping("/{id}/actions/audits")
    public ResponseEntity<Page<AiActionAudit>> actionAudits(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) Long actionId,
                                                            @RequestParam(required = false) String eventType,
                                                            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        requireInvestigation(id, user.getId());
        int safeSize = Math.max(1, Math.min(100, size));
        PageRequest pageable = PageRequest.of(Math.max(0, page), safeSize, Sort.by("createdAt").descending());
        Page<AiActionAudit> audits = aiActionAuditRepository.search(
                id,
                user.getId(),
                actionId,
                normalizeUpper(eventType),
                pageable
        );
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/{id}/actions/{actionId}/rollback-runs")
    public ResponseEntity<List<AiRollbackRun>> rollbackRuns(@PathVariable Long id,
                                                            @PathVariable Long actionId,
                                                            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        requireInvestigation(id, user.getId());
        aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));
        return ResponseEntity.ok(
                aiRollbackRunRepository.findByActionPlanIdAndInvestigationIdAndUserIdOrderByCreatedAtDesc(
                        actionId,
                        id,
                        user.getId()
                )
        );
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<Map<String, Object>>> timeline(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        AiInvestigation investigation = requireInvestigation(id, user.getId());

        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent(
                nonNullTime(investigation.getCreatedAt(), investigation.getStartedAt()),
                "INVESTIGATION",
                "Investigation Created",
                investigation.getTitle(),
                investigation.getId(),
                Map.of(
                        "status", investigation.getStatus(),
                        "severity", investigation.getSeverity(),
                        "triggerSource", investigation.getTriggerSource()
                )
        ));
        if (investigation.getClosedAt() != null) {
            events.add(new TimelineEvent(
                    investigation.getClosedAt(),
                    "INVESTIGATION",
                    "Investigation Closed",
                    investigation.getTitle(),
                    investigation.getId(),
                    Map.of(
                            "status", "CLOSED",
                            "severity", investigation.getSeverity()
                    )
            ));
        }

        aiObservationRepository.findByInvestigationIdAndUserIdOrderByObservedAtAsc(id, user.getId())
                .forEach(obs -> events.add(new TimelineEvent(
                        nonNullTime(obs.getObservedAt(), obs.getCreatedAt()),
                        "OBSERVATION",
                        "Observation: " + safe(obs.getType(), "METRIC"),
                        safe(obs.getMetricName(), safe(obs.getSourceRef(), "N/A")),
                        obs.getId(),
                        Map.of(
                                "hostname", safe(obs.getHostname(), "unknown-host"),
                                "metricValue", obs.getMetricValue() == null ? "-" : obs.getMetricValue(),
                                "confidence", obs.getConfidence() == null ? "-" : obs.getConfidence()
                        )
                )));

        aiHypothesisRepository.findByInvestigationIdAndUserIdOrderByRankOrderAsc(id, user.getId())
                .forEach(h -> events.add(new TimelineEvent(
                        nonNullTime(h.getUpdatedAt(), h.getCreatedAt()),
                        "HYPOTHESIS",
                        "Hypothesis #" + h.getRankOrder(),
                        h.getTitle(),
                        h.getId(),
                        Map.of(
                                "status", safe(h.getStatus(), "CANDIDATE"),
                                "confidence", h.getConfidence() == null ? "-" : h.getConfidence()
                        )
                )));

        aiActionPlanRepository.findByInvestigationIdAndUserIdOrderByCreatedAtAsc(id, user.getId())
                .forEach(action -> events.add(new TimelineEvent(
                        nonNullTime(action.getUpdatedAt(), action.getCreatedAt()),
                        "ACTION_PLAN",
                        safe(action.getActionType(), "ACTION"),
                        action.getTitle(),
                        action.getId(),
                        Map.of(
                                "status", safe(action.getStatus(), "PROPOSED"),
                                "riskLevel", safe(action.getRiskLevel(), "MEDIUM"),
                                "requiresApproval", action.isRequiresApproval(),
                                "approvedBy", safe(action.getApprovedBy(), "-"),
                                "approvedAt", action.getApprovedAt() == null ? "-" : action.getApprovedAt().toString(),
                                "retryCount", action.getRetryCount() == null ? 0 : action.getRetryCount()
                        )
                )));

        aiActionRunRepository.findByInvestigationIdAndUserIdOrderByCreatedAtAsc(id, user.getId())
                .forEach(run -> events.add(new TimelineEvent(
                        nonNullTime(run.getStartedAt(), run.getCreatedAt()),
                        "ACTION_RUN",
                        "Execution: " + safe(run.getStatus(), "PENDING"),
                        safe(run.getExecutor(), "unknown-executor"),
                        run.getId(),
                        Map.of(
                                "executionMode", safe(run.getExecutionMode(), "MANUAL"),
                                "endedAt", run.getEndedAt() == null ? "-" : run.getEndedAt().toString()
                        )
                )));

        aiRollbackRunRepository.findByInvestigationIdAndUserIdOrderByCreatedAtDesc(id, user.getId())
                .forEach(run -> events.add(new TimelineEvent(
                        nonNullTime(run.getStartedAt(), run.getCreatedAt()),
                        "ROLLBACK_RUN",
                        (run.isDrillMode() ? "Rollback Drill: " : "Rollback Execute: ") + safe(run.getStatus(), "PENDING"),
                        safe(run.getExecutor(), "unknown-executor"),
                        run.getId(),
                        Map.of(
                                "executionMode", safe(run.getExecutionMode(), "MANUAL"),
                                "drillMode", run.isDrillMode(),
                                "endedAt", run.getEndedAt() == null ? "-" : run.getEndedAt().toString()
                        )
                )));

        aiActionAuditRepository.search(id, user.getId(), null, null, PageRequest.of(0, 100))
                .getContent()
                .forEach(audit -> events.add(new TimelineEvent(
                        audit.getCreatedAt(),
                        "ACTION_AUDIT",
                        "Audit: " + safe(audit.getEventType(), "ACTION_EVENT"),
                        safe(audit.getActor(), "system"),
                        audit.getId(),
                        Map.of(
                                "decision", safe(audit.getDecision(), "-"),
                                "riskLevel", safe(audit.getRiskLevel(), "-")
                        )
                )));

        aiReportSnapshotRepository.findByInvestigationIdAndUserIdOrderByVersionNoDesc(id, user.getId())
                .forEach(snapshot -> events.add(new TimelineEvent(
                        snapshot.getCreatedAt(),
                        "REPORT_SNAPSHOT",
                        "Report Snapshot v" + snapshot.getVersionNo(),
                        safe(snapshot.getFormat(), "MARKDOWN"),
                        snapshot.getId(),
                        Map.of("createdBy", safe(snapshot.getCreatedBy(), "AI"))
                )));

        List<Map<String, Object>> timeline = events.stream()
                .sorted(Comparator.comparing(TimelineEvent::time, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(this::toTimelineMap)
                .toList();
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> close(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        AiInvestigation investigation = requireInvestigation(id, user.getId());
        investigation.setStatus("CLOSED");
        investigation.setClosedAt(LocalDateTime.now());
        investigation.setUpdatedAt(LocalDateTime.now());
        aiInvestigationRepository.save(investigation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", investigation.getId());
        result.put("status", investigation.getStatus());
        result.put("closedAt", investigation.getClosedAt());
        investigationEventPublisher.publish(
                user.getId(),
                "INVESTIGATION_CLOSED",
                investigation.getId(),
                "Investigation closed",
                Map.of("closedAt", investigation.getClosedAt().toString())
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/ai-generate")
    public ResponseEntity<Map<String, Object>> generateStructuredResult(@PathVariable Long id,
                                                                        @RequestBody(required = false) AiInvestigationGenerateRequest request,
                                                                        Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        String promptHint = request == null ? null : normalize(request.getPromptHint());
        boolean includePostmortem = request != null && Boolean.TRUE.equals(request.getIncludePostmortem());
        Map<String, Object> result = investigationIntelligenceService.generateAndPersist(
                id,
                user.getId(),
                user.getUsername(),
                promptHint,
                includePostmortem
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/postmortem-draft")
    public ResponseEntity<Map<String, Object>> generatePostmortem(@PathVariable Long id,
                                                                  @RequestBody(required = false) AiPostmortemGenerateRequest request,
                                                                  Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        String additionalContext = request == null ? null : normalize(request.getAdditionalContext());
        Map<String, Object> result = investigationIntelligenceService.generatePostmortemDraft(
                id,
                user.getId(),
                user.getUsername(),
                additionalContext
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/observations")
    public ResponseEntity<Map<String, Object>> createObservation(@PathVariable Long id,
                                                                 @Valid @RequestBody AiObservationCreateRequest request,
                                                                 Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        AiInvestigation investigation = requireInvestigation(id, user.getId());
        if ("CLOSED".equalsIgnoreCase(investigation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "调查已关闭，不能追加证据");
        }

        AiObservation observation = new AiObservation();
        observation.setInvestigationId(id);
        observation.setUserId(user.getId());
        observation.setType(resolveObservationType(request.getType()));
        observation.setSourceRef(normalize(request.getSourceRef()));
        observation.setHostname(normalize(request.getHostname()));
        observation.setMetricName(normalize(request.getMetricName()));
        observation.setMetricValue(request.getMetricValue());
        observation.setObservedAt(request.getObservedAt() == null ? LocalDateTime.now() : request.getObservedAt());
        observation.setConfidence(request.getConfidence());
        observation.setPayloadJson(normalize(request.getPayloadJson()));
        observation.setCreatedAt(LocalDateTime.now());
        AiObservation saved = aiObservationRepository.save(observation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("type", saved.getType());
        result.put("metricName", saved.getMetricName());
        result.put("metricValue", saved.getMetricValue());
        result.put("observedAt", saved.getObservedAt());
        investigationEventPublisher.publish(
                user.getId(),
                "OBSERVATION_CREATED",
                id,
                "Observation appended",
                Map.of(
                        "observationId", saved.getId(),
                        "type", safe(saved.getType(), "METRIC"),
                        "metric", safe(saved.getMetricName(), "N/A")
                )
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/hypotheses")
    public ResponseEntity<Map<String, Object>> createHypothesis(@PathVariable Long id,
                                                                @Valid @RequestBody AiHypothesisCreateRequest request,
                                                                Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        AiInvestigation investigation = requireInvestigation(id, user.getId());
        if ("CLOSED".equalsIgnoreCase(investigation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "调查已关闭，不能新增假设");
        }

        int nextRank = aiHypothesisRepository.findByInvestigationIdAndUserIdOrderByRankOrderAsc(id, user.getId())
                .stream()
                .map(AiHypothesis::getRankOrder)
                .filter(x -> x != null && x >= 0)
                .max(Integer::compareTo)
                .map(x -> x + 1)
                .orElse(1);

        AiHypothesis hypothesis = new AiHypothesis();
        hypothesis.setInvestigationId(id);
        hypothesis.setUserId(user.getId());
        hypothesis.setTitle(normalize(request.getTitle()));
        hypothesis.setReasoning(normalize(request.getReasoning()));
        hypothesis.setConfidence(request.getConfidence());
        hypothesis.setRankOrder(request.getRankOrder() == null ? nextRank : Math.max(0, request.getRankOrder()));
        hypothesis.setStatus(resolveHypothesisStatus(request.getStatus()));
        hypothesis.setCreatedAt(LocalDateTime.now());
        hypothesis.setUpdatedAt(LocalDateTime.now());
        AiHypothesis saved = aiHypothesisRepository.save(hypothesis);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("title", saved.getTitle());
        result.put("status", saved.getStatus());
        result.put("rankOrder", saved.getRankOrder());
        result.put("confidence", saved.getConfidence());
        investigationEventPublisher.publish(
                user.getId(),
                "HYPOTHESIS_CREATED",
                id,
                "Hypothesis created",
                Map.of(
                        "hypothesisId", saved.getId(),
                        "status", saved.getStatus(),
                        "rankOrder", saved.getRankOrder()
                )
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions")
    public ResponseEntity<Map<String, Object>> createAction(@PathVariable Long id,
                                                            @Valid @RequestBody AiActionPlanCreateRequest request,
                                                            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        AiInvestigation investigation = requireInvestigation(id, user.getId());
        if ("CLOSED".equalsIgnoreCase(investigation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "调查已关闭，不能新增动作");
        }

        AiActionPlan actionPlan = new AiActionPlan();
        actionPlan.setInvestigationId(investigation.getId());
        actionPlan.setUserId(user.getId());
        actionPlan.setHypothesisId(request.getHypothesisId());
        actionPlan.setActionType(normalizeUpper(request.getActionType()));
        actionPlan.setTitle(normalize(request.getTitle()));
        actionPlan.setCommandText(normalize(request.getCommandText()));
        actionPlan.setRunbookRef(normalize(request.getRunbookRef()));
        String riskLevel = resolveRiskLevel(request.getRiskLevel());
        actionPlan.setRiskLevel(riskLevel);
        boolean requiresApproval = resolveRequiresApproval(riskLevel, request.getRequiresApproval());
        String rollbackPlan = normalize(request.getRollbackPlan());
        if ("HIGH".equals(riskLevel) && rollbackPlan == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HIGH 风险动作必须提供 rollbackPlan");
        }
        actionPlan.setRequiresApproval(requiresApproval);
        actionPlan.setStatus(requiresApproval ? "PROPOSED" : "APPROVED");
        actionPlan.setRollbackPlan(rollbackPlan);
        actionPlan.setCreatedAt(LocalDateTime.now());
        actionPlan.setUpdatedAt(LocalDateTime.now());
        AiActionPlan saved = aiActionPlanRepository.save(actionPlan);

        if (!requiresApproval) {
            saved.setApprovedBy(user.getUsername());
            saved.setApprovedAt(LocalDateTime.now());
            saved.setApprovalNote("AUTO_APPROVED_BY_POLICY");
            saved.setUpdatedAt(LocalDateTime.now());
            saved = aiActionPlanRepository.save(saved);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("status", saved.getStatus());
        result.put("requiresApproval", saved.isRequiresApproval());
        result.put("riskLevel", saved.getRiskLevel());
        investigationEventPublisher.publish(
                user.getId(),
                "ACTION_PLAN_CREATED",
                id,
                "Action plan created",
                Map.of(
                        "actionId", saved.getId(),
                        "status", saved.getStatus(),
                        "riskLevel", safe(saved.getRiskLevel(), "MEDIUM")
                )
        );
        recordActionAudit(
                id,
                saved.getId(),
                user,
                "ACTION_PLAN_CREATED",
                "ACCEPTED",
                saved.getRiskLevel(),
                Map.of(
                        "status", saved.getStatus(),
                        "requiresApproval", saved.isRequiresApproval(),
                        "actionType", safe(saved.getActionType(), "RUNBOOK"),
                        "title", safe(saved.getTitle(), "-")
                )
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions/{actionId}/approve")
    public ResponseEntity<Map<String, Object>> approveAction(@PathVariable Long id,
                                                             @PathVariable Long actionId,
                                                             @RequestBody(required = false) AiActionApproveRequest request,
                                                             Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));

        if ("EXECUTED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作已执行，无法重复审批");
        }

        if ("APPROVED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作已审批，无法重复审批");
        }

        String approvalNote = normalize(request == null ? null : request.getNote());
        if ("HIGH".equalsIgnoreCase(actionPlan.getRiskLevel()) && approvalNote == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HIGH 风险动作审批必须填写 note");
        }

        actionPlan.setStatus("APPROVED");
        actionPlan.setApprovedBy(user.getUsername());
        actionPlan.setApprovedAt(LocalDateTime.now());
        actionPlan.setApprovalNote(approvalNote);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        aiActionPlanRepository.save(actionPlan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", actionPlan.getId());
        result.put("status", actionPlan.getStatus());
        result.put("approvedBy", actionPlan.getApprovedBy());
        result.put("approvedAt", actionPlan.getApprovedAt());
        result.put("updatedAt", actionPlan.getUpdatedAt());
        investigationEventPublisher.publish(
                user.getId(),
                "ACTION_APPROVED",
                id,
                "Action approved",
                Map.of(
                        "actionId", actionPlan.getId(),
                        "approvedBy", safe(actionPlan.getApprovedBy(), "unknown")
                )
        );
        recordActionAudit(
                id,
                actionPlan.getId(),
                user,
                "ACTION_APPROVED",
                "APPROVED",
                actionPlan.getRiskLevel(),
                Map.of(
                        "approvedBy", safe(actionPlan.getApprovedBy(), "unknown"),
                        "approvalNote", safe(actionPlan.getApprovalNote(), "-")
                )
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions/{actionId}/execute")
    public ResponseEntity<Map<String, Object>> executeAction(@PathVariable Long id,
                                                             @PathVariable Long actionId,
                                                             @RequestBody(required = false) AiActionExecuteRequest request,
                                                             Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));

        if ("EXECUTED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作已执行，不能重复执行");
        }
        if ("ROLLED_BACK".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作已回滚，不能再次执行");
        }

        if (actionPlan.isRequiresApproval()
                && !"APPROVED".equalsIgnoreCase(actionPlan.getStatus())
                && !"FAILED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作未审批，不能执行");
        }
        if ("HIGH".equalsIgnoreCase(actionPlan.getRiskLevel()) && normalize(actionPlan.getRollbackPlan()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HIGH 风险动作缺少 rollbackPlan，不能执行");
        }

        LocalDateTime now = LocalDateTime.now();
        String runStatus = resolveExecutionStatus(request == null ? null : request.getStatus());

        AiActionRun actionRun = new AiActionRun();
        actionRun.setActionPlanId(actionPlan.getId());
        actionRun.setInvestigationId(id);
        actionRun.setUserId(user.getId());
        actionRun.setExecutor(user.getUsername());
        actionRun.setExecutionMode(resolveExecutionMode(request == null ? null : request.getExecutionMode()));
        actionRun.setStatus(runStatus);
        actionRun.setOutputText(normalize(request == null ? null : request.getOutputText()));
        actionRun.setErrorMessage(normalize(request == null ? null : request.getErrorMessage()));
        actionRun.setStartedAt(now);
        actionRun.setEndedAt(now);
        actionRun.setCreatedAt(now);
        AiActionRun savedRun = aiActionRunRepository.save(actionRun);

        actionPlan.setStatus("SUCCESS".equalsIgnoreCase(runStatus) ? "EXECUTED" : "FAILED");
        actionPlan.setUpdatedAt(LocalDateTime.now());
        aiActionPlanRepository.save(actionPlan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", actionPlan.getId());
        result.put("actionStatus", actionPlan.getStatus());
        result.put("runId", savedRun.getId());
        result.put("runStatus", savedRun.getStatus());
        investigationEventPublisher.publish(
                user.getId(),
                "ACTION_EXECUTED",
                id,
                "Action execution finished",
                Map.of(
                        "actionId", actionPlan.getId(),
                        "runId", savedRun.getId(),
                        "runStatus", safe(savedRun.getStatus(), "UNKNOWN")
                )
        );
        recordActionAudit(
                id,
                actionPlan.getId(),
                user,
                "ACTION_EXECUTED",
                safe(savedRun.getStatus(), "UNKNOWN"),
                actionPlan.getRiskLevel(),
                Map.of(
                        "runId", savedRun.getId(),
                        "executionMode", safe(savedRun.getExecutionMode(), "MANUAL"),
                        "actionStatus", safe(actionPlan.getStatus(), "-")
                )
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions/{actionId}/retry")
    public ResponseEntity<Map<String, Object>> retryAction(@PathVariable Long id,
                                                           @PathVariable Long actionId,
                                                           @RequestBody(required = false) AiActionRetryRequest request,
                                                           Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));

        if (!"FAILED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅 FAILED 状态动作允许重试");
        }

        LocalDateTime now = LocalDateTime.now();
        String runStatus = resolveExecutionStatus(request == null ? null : request.getStatus());

        AiActionRun actionRun = new AiActionRun();
        actionRun.setActionPlanId(actionPlan.getId());
        actionRun.setInvestigationId(id);
        actionRun.setUserId(user.getId());
        actionRun.setExecutor(user.getUsername());
        actionRun.setExecutionMode(resolveExecutionMode(request == null ? null : request.getExecutionMode()));
        actionRun.setStatus(runStatus);
        actionRun.setOutputText(normalize(request == null ? null : request.getOutputText()));
        actionRun.setErrorMessage(normalize(request == null ? null : request.getErrorMessage()));
        actionRun.setStartedAt(now);
        actionRun.setEndedAt(now);
        actionRun.setCreatedAt(now);
        AiActionRun savedRun = aiActionRunRepository.save(actionRun);

        actionPlan.setStatus("SUCCESS".equalsIgnoreCase(runStatus) ? "EXECUTED" : "FAILED");
        actionPlan.setRetryCount((actionPlan.getRetryCount() == null ? 0 : actionPlan.getRetryCount()) + 1);
        actionPlan.setUpdatedAt(LocalDateTime.now());
        aiActionPlanRepository.save(actionPlan);

        investigationEventPublisher.publish(
                user.getId(),
                "ACTION_RETRY",
                id,
                "Action retried",
                Map.of(
                        "actionId", actionPlan.getId(),
                        "runId", savedRun.getId(),
                        "runStatus", safe(savedRun.getStatus(), "UNKNOWN"),
                        "retryCount", actionPlan.getRetryCount() == null ? 0 : actionPlan.getRetryCount()
                )
        );
        recordActionAudit(
                id,
                actionPlan.getId(),
                user,
                "ACTION_RETRY",
                safe(savedRun.getStatus(), "UNKNOWN"),
                actionPlan.getRiskLevel(),
                Map.of(
                        "runId", savedRun.getId(),
                        "retryCount", actionPlan.getRetryCount() == null ? 0 : actionPlan.getRetryCount(),
                        "executionMode", safe(savedRun.getExecutionMode(), "MANUAL")
                )
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", actionPlan.getId());
        result.put("actionStatus", actionPlan.getStatus());
        result.put("retryCount", actionPlan.getRetryCount());
        result.put("runId", savedRun.getId());
        result.put("runStatus", savedRun.getStatus());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions/{actionId}/rollback-drill")
    public ResponseEntity<Map<String, Object>> rollbackDrill(@PathVariable Long id,
                                                             @PathVariable Long actionId,
                                                             @RequestBody(required = false) AiActionRollbackRequest request,
                                                             Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));
        return ResponseEntity.ok(runRollback(actionPlan, id, user, request, true));
    }

    @PostMapping("/{id}/actions/{actionId}/rollback-execute")
    public ResponseEntity<Map<String, Object>> rollbackExecute(@PathVariable Long id,
                                                               @PathVariable Long actionId,
                                                               @RequestBody(required = false) AiActionRollbackRequest request,
                                                               Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));
        return ResponseEntity.ok(runRollback(actionPlan, id, user, request, false));
    }

    @PostMapping("/{id}/snapshots")
    public ResponseEntity<Map<String, Object>> createSnapshot(@PathVariable Long id,
                                                              @Valid @RequestBody AiReportSnapshotCreateRequest request,
                                                              Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireUserResourceOperator(user);
        requireInvestigation(id, user.getId());

        String markdown = normalize(request.getReportMarkdown());
        String reportJson = normalize(request.getReportJson());
        if (markdown == null && reportJson == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportMarkdown 或 reportJson 至少传一个");
        }

        int nextVersion = aiReportSnapshotRepository
                .findFirstByInvestigationIdAndUserIdOrderByVersionNoDesc(id, user.getId())
                .map(snapshot -> snapshot.getVersionNo() + 1)
                .orElse(1);

        AiReportSnapshot snapshot = new AiReportSnapshot();
        snapshot.setInvestigationId(id);
        snapshot.setUserId(user.getId());
        snapshot.setVersionNo(nextVersion);
        snapshot.setFormat(resolveSnapshotFormat(request.getFormat(), markdown, reportJson));
        snapshot.setReportMarkdown(markdown);
        snapshot.setReportJson(reportJson);
        snapshot.setCreatedBy(normalize(request.getCreatedBy()) == null ? user.getUsername() : normalize(request.getCreatedBy()));
        snapshot.setCreatedAt(LocalDateTime.now());
        AiReportSnapshot saved = aiReportSnapshotRepository.save(snapshot);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("versionNo", saved.getVersionNo());
        result.put("format", saved.getFormat());
        result.put("createdBy", saved.getCreatedBy());
        result.put("createdAt", saved.getCreatedAt());
        investigationEventPublisher.publish(
                user.getId(),
                "SNAPSHOT_CREATED",
                id,
                "Report snapshot created",
                Map.of(
                        "snapshotId", saved.getId(),
                        "versionNo", saved.getVersionNo(),
                        "format", safe(saved.getFormat(), "MARKDOWN")
                )
        );
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> runRollback(AiActionPlan actionPlan,
                                            Long investigationId,
                                            User user,
                                            AiActionRollbackRequest request,
                                            boolean drillMode) {
        String rollbackPlan = normalize(actionPlan.getRollbackPlan());
        if (rollbackPlan == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该动作缺少 rollbackPlan，无法回滚");
        }
        if (!"EXECUTED".equalsIgnoreCase(actionPlan.getStatus()) && !drillMode) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅 EXECUTED 状态动作支持正式回滚");
        }

        LocalDateTime now = LocalDateTime.now();
        String runStatus = resolveExecutionStatus(request == null ? null : request.getStatus());
        AiRollbackRun rollbackRun = new AiRollbackRun();
        rollbackRun.setActionPlanId(actionPlan.getId());
        rollbackRun.setInvestigationId(investigationId);
        rollbackRun.setUserId(user.getId());
        rollbackRun.setExecutor(user.getUsername());
        rollbackRun.setDrillMode(drillMode);
        rollbackRun.setExecutionMode(resolveExecutionMode(request == null ? null : request.getExecutionMode()));
        rollbackRun.setStatus(runStatus);
        rollbackRun.setNoteText(normalize(request == null ? null : request.getNote()));
        rollbackRun.setOutputText(normalize(request == null ? null : request.getOutputText()));
        rollbackRun.setErrorMessage(normalize(request == null ? null : request.getErrorMessage()));
        rollbackRun.setStartedAt(now);
        rollbackRun.setEndedAt(now);
        rollbackRun.setCreatedAt(now);
        AiRollbackRun savedRun = aiRollbackRunRepository.save(rollbackRun);

        if (!drillMode && "SUCCESS".equalsIgnoreCase(runStatus)) {
            actionPlan.setStatus("ROLLED_BACK");
            actionPlan.setUpdatedAt(LocalDateTime.now());
            aiActionPlanRepository.save(actionPlan);
        }

        String eventType = drillMode ? "ROLLBACK_DRILL" : "ROLLBACK_EXECUTED";
        investigationEventPublisher.publish(
                user.getId(),
                eventType,
                investigationId,
                drillMode ? "Rollback drill finished" : "Rollback execution finished",
                Map.of(
                        "actionId", actionPlan.getId(),
                        "rollbackRunId", savedRun.getId(),
                        "status", safe(savedRun.getStatus(), "UNKNOWN")
                )
        );

        recordActionAudit(
                investigationId,
                actionPlan.getId(),
                user,
                eventType,
                safe(savedRun.getStatus(), "UNKNOWN"),
                actionPlan.getRiskLevel(),
                Map.of(
                        "rollbackRunId", savedRun.getId(),
                        "drillMode", savedRun.isDrillMode(),
                        "executionMode", safe(savedRun.getExecutionMode(), "MANUAL"),
                        "note", safe(savedRun.getNoteText(), "-")
                )
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", actionPlan.getId());
        result.put("actionStatus", actionPlan.getStatus());
        result.put("rollbackRunId", savedRun.getId());
        result.put("rollbackStatus", savedRun.getStatus());
        result.put("drillMode", savedRun.isDrillMode());
        return result;
    }

    private void recordActionAudit(Long investigationId,
                                   Long actionId,
                                   User actor,
                                   String eventType,
                                   String decision,
                                   String riskLevel,
                                   Map<String, Object> details) {
        AiActionAudit audit = new AiActionAudit();
        audit.setInvestigationId(investigationId);
        audit.setActionPlanId(actionId);
        audit.setUserId(actor.getId());
        audit.setEventType(eventType);
        audit.setActor(actor.getUsername());
        audit.setDecision(decision);
        audit.setRiskLevel(riskLevel);
        audit.setDetailJson(toJson(details));
        audit.setCreatedAt(LocalDateTime.now());
        aiActionAuditRepository.save(audit);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Map.of() : data);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"detail_json_serialize_failed\"}";
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeUpper(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String resolveSeverity(String severityInput) {
        String normalized = normalizeUpper(severityInput);
        if (normalized == null) {
            return "P2";
        }
        if (!"P1".equals(normalized) && !"P2".equals(normalized) && !"P3".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "severity 仅支持 P1/P2/P3");
        }
        return normalized;
    }

    private String resolveHypothesisStatus(String statusInput) {
        String normalized = normalizeUpper(statusInput);
        if (normalized == null) {
            return "CANDIDATE";
        }
        if (!"CANDIDATE".equals(normalized) && !"CONFIRMED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅支持 CANDIDATE/CONFIRMED/REJECTED");
        }
        return normalized;
    }

    private String resolveObservationType(String typeInput) {
        String normalized = normalizeUpper(typeInput);
        if (normalized == null) {
            return "METRIC";
        }
        if (!"METRIC".equals(normalized)
                && !"LOG".equals(normalized)
                && !"TRACE".equals(normalized)
                && !"CHANGE".equals(normalized)
                && !"EVENT".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type 仅支持 METRIC/LOG/TRACE/CHANGE/EVENT");
        }
        return normalized;
    }

    private String resolveRiskLevel(String riskInput) {
        String normalized = normalizeUpper(riskInput);
        if (normalized == null) {
            return "MEDIUM";
        }
        if (!"LOW".equals(normalized) && !"MEDIUM".equals(normalized) && !"HIGH".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "riskLevel 仅支持 LOW/MEDIUM/HIGH");
        }
        return normalized;
    }

    private boolean resolveRequiresApproval(String riskLevel, Boolean requested) {
        String normalizedRisk = resolveRiskLevel(riskLevel);
        if ("HIGH".equals(normalizedRisk) || "MEDIUM".equals(normalizedRisk)) {
            return true;
        }
        if (requested == null) {
            return false;
        }
        return requested;
    }

    private String resolveExecutionStatus(String statusInput) {
        String normalized = normalizeUpper(statusInput);
        if (normalized == null) {
            return "SUCCESS";
        }
        if (!"SUCCESS".equals(normalized) && !"FAILED".equals(normalized) && !"PENDING".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅支持 SUCCESS/FAILED/PENDING");
        }
        return normalized;
    }

    private String resolveExecutionMode(String modeInput) {
        String normalized = normalizeUpper(modeInput);
        if (normalized == null) {
            return "MANUAL";
        }
        if (!"MANUAL".equals(normalized) && !"AUTO".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "executionMode 仅支持 MANUAL/AUTO");
        }
        return normalized;
    }

    private String resolveSnapshotFormat(String input, String markdown, String reportJson) {
        String normalized = normalizeUpper(input);
        if (normalized == null) {
            if (markdown != null && reportJson != null) return "BOTH";
            return markdown != null ? "MARKDOWN" : "JSON";
        }
        if (!"MARKDOWN".equals(normalized) && !"JSON".equals(normalized) && !"BOTH".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format 仅支持 MARKDOWN/JSON/BOTH");
        }
        if ("MARKDOWN".equals(normalized) && markdown == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format=MARKDOWN 时 reportMarkdown 不能为空");
        }
        if ("JSON".equals(normalized) && reportJson == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format=JSON 时 reportJson 不能为空");
        }
        if ("BOTH".equals(normalized) && (markdown == null || reportJson == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "format=BOTH 时 reportMarkdown 和 reportJson 都不能为空");
        }
        return normalized;
    }

    private LocalDateTime nonNullTime(LocalDateTime first, LocalDateTime second) {
        if (first != null) return first;
        if (second != null) return second;
        return LocalDateTime.now();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private AiInvestigation requireInvestigation(Long id, Long userId) {
        return aiInvestigationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "调查不存在"));
    }

    private Map<String, Object> toTimelineMap(TimelineEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("time", event.time());
        map.put("category", event.category());
        map.put("title", event.title());
        map.put("detail", event.detail());
        map.put("refId", event.refId());
        map.put("metadata", event.metadata());
        return map;
    }

    private record TimelineEvent(
            LocalDateTime time,
            String category,
            String title,
            String detail,
            Long refId,
            Map<String, Object> metadata
    ) {
    }
}
