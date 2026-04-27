package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AiInvestigationCreateRequest;
import com.aiops.monitor.model.entity.AiInvestigation;
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
import java.util.LinkedHashMap;
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
        AiInvestigation investigation = aiInvestigationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "调查不存在"));

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

    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> close(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        AiInvestigation investigation = aiInvestigationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "调查不存在"));
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
}
