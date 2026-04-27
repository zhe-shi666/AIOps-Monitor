package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AiActionExecuteRequest;
import com.aiops.monitor.model.dto.AiActionPlanCreateRequest;
import com.aiops.monitor.model.dto.AiInvestigationCreateRequest;
import com.aiops.monitor.model.dto.AiReportSnapshotCreateRequest;
import com.aiops.monitor.model.entity.AiActionPlan;
import com.aiops.monitor.model.entity.AiActionRun;
import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.AiReportSnapshot;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.AiActionPlanRepository;
import com.aiops.monitor.repository.AiActionRunRepository;
import com.aiops.monitor.repository.AiHypothesisRepository;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.AiObservationRepository;
import com.aiops.monitor.repository.AiReportSnapshotRepository;
import com.aiops.monitor.service.CurrentUserService;
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
    private final AiReportSnapshotRepository aiReportSnapshotRepository;
    private final CurrentUserService currentUserService;

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

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody(required = false) AiInvestigationCreateRequest request,
                                                      Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
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
        result.put("latestSnapshot", aiReportSnapshotRepository.findFirstByInvestigationIdAndUserIdOrderByVersionNoDesc(id, user.getId()).orElse(null));
        result.put("snapshots", aiReportSnapshotRepository.findByInvestigationIdAndUserIdOrderByVersionNoDesc(id, user.getId()));
        return ResponseEntity.ok(result);
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
                                "requiresApproval", action.isRequiresApproval()
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
                                "endedAt", run.getEndedAt()
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
        AiInvestigation investigation = requireInvestigation(id, user.getId());
        investigation.setStatus("CLOSED");
        investigation.setClosedAt(LocalDateTime.now());
        investigation.setUpdatedAt(LocalDateTime.now());
        aiInvestigationRepository.save(investigation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", investigation.getId());
        result.put("status", investigation.getStatus());
        result.put("closedAt", investigation.getClosedAt());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions")
    public ResponseEntity<Map<String, Object>> createAction(@PathVariable Long id,
                                                            @Valid @RequestBody AiActionPlanCreateRequest request,
                                                            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
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
        actionPlan.setRiskLevel(resolveRiskLevel(request.getRiskLevel()));
        boolean requiresApproval = request.getRequiresApproval() == null || request.getRequiresApproval();
        actionPlan.setRequiresApproval(requiresApproval);
        actionPlan.setStatus(requiresApproval ? "PROPOSED" : "APPROVED");
        actionPlan.setRollbackPlan(normalize(request.getRollbackPlan()));
        actionPlan.setCreatedAt(LocalDateTime.now());
        actionPlan.setUpdatedAt(LocalDateTime.now());
        AiActionPlan saved = aiActionPlanRepository.save(actionPlan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("status", saved.getStatus());
        result.put("requiresApproval", saved.isRequiresApproval());
        result.put("riskLevel", saved.getRiskLevel());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions/{actionId}/approve")
    public ResponseEntity<Map<String, Object>> approveAction(@PathVariable Long id,
                                                             @PathVariable Long actionId,
                                                             Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));

        if ("EXECUTED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作已执行，无法重复审批");
        }

        actionPlan.setStatus("APPROVED");
        actionPlan.setUpdatedAt(LocalDateTime.now());
        aiActionPlanRepository.save(actionPlan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", actionPlan.getId());
        result.put("status", actionPlan.getStatus());
        result.put("updatedAt", actionPlan.getUpdatedAt());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/actions/{actionId}/execute")
    public ResponseEntity<Map<String, Object>> executeAction(@PathVariable Long id,
                                                             @PathVariable Long actionId,
                                                             @RequestBody(required = false) AiActionExecuteRequest request,
                                                             Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        requireInvestigation(id, user.getId());
        AiActionPlan actionPlan = aiActionPlanRepository.findByIdAndInvestigationIdAndUserId(actionId, id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "动作不存在"));

        if ("EXECUTED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作已执行，不能重复执行");
        }

        if (actionPlan.isRequiresApproval()
                && !"APPROVED".equalsIgnoreCase(actionPlan.getStatus())
                && !"FAILED".equalsIgnoreCase(actionPlan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "动作未审批，不能执行");
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
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/snapshots")
    public ResponseEntity<Map<String, Object>> createSnapshot(@PathVariable Long id,
                                                              @Valid @RequestBody AiReportSnapshotCreateRequest request,
                                                              Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
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
        return ResponseEntity.ok(result);
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

    private String resolveExecutionStatus(String statusInput) {
        String normalized = normalizeUpper(statusInput);
        if (normalized == null) {
            return "SUCCESS";
        }
        if (!"SUCCESS".equals(normalized) && !"FAILED".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅支持 SUCCESS/FAILED");
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
