package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AiActionPlan;
import com.aiops.monitor.model.entity.AiHypothesis;
import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.AiObservation;
import com.aiops.monitor.model.entity.AiReportSnapshot;
import com.aiops.monitor.repository.AiActionPlanRepository;
import com.aiops.monitor.repository.AiHypothesisRepository;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.AiObservationRepository;
import com.aiops.monitor.repository.AiReportSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationIntelligenceService {

    private final AiInvestigationRepository aiInvestigationRepository;
    private final AiObservationRepository aiObservationRepository;
    private final AiHypothesisRepository aiHypothesisRepository;
    private final AiActionPlanRepository aiActionPlanRepository;
    private final AiReportSnapshotRepository aiReportSnapshotRepository;
    private final AiService aiService;
    private final InvestigationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> generateAndPersist(Long investigationId,
                                                  Long userId,
                                                  String operator,
                                                  String promptHint,
                                                  boolean includePostmortem) {
        AiInvestigation investigation = requireInvestigation(investigationId, userId);
        List<AiObservation> observations = aiObservationRepository
                .findByInvestigationIdAndUserIdOrderByObservedAtAsc(investigationId, userId);

        StructuredAnalysis analysis = runAnalysis(investigation, observations, promptHint);

        investigation.setSummary(analysis.summary());
        investigation.setRootCause(analysis.rootCause());
        investigation.setConfidence(analysis.confidence());
        investigation.setSeverity(mapRiskToSeverity(analysis.riskLevel()));
        investigation.setStatus("ANALYZING");
        investigation.setUpdatedAt(LocalDateTime.now());
        aiInvestigationRepository.save(investigation);

        int nextRank = aiHypothesisRepository.findByInvestigationIdAndUserIdOrderByRankOrderAsc(investigationId, userId)
                .stream()
                .map(AiHypothesis::getRankOrder)
                .filter(x -> x != null && x >= 0)
                .max(Integer::compareTo)
                .map(x -> x + 1)
                .orElse(1);

        int createdHypothesis = 0;
        for (HypothesisDraft draft : analysis.hypotheses()) {
            AiHypothesis hypothesis = new AiHypothesis();
            hypothesis.setInvestigationId(investigationId);
            hypothesis.setUserId(userId);
            hypothesis.setTitle(nonBlank(draft.title(), "Hypothesis"));
            hypothesis.setReasoning(draft.reasoning());
            hypothesis.setConfidence(draft.confidence());
            hypothesis.setRankOrder(nextRank++);
            hypothesis.setStatus(normalizeHypothesisStatus(draft.status()));
            hypothesis.setCreatedAt(LocalDateTime.now());
            hypothesis.setUpdatedAt(LocalDateTime.now());
            aiHypothesisRepository.save(hypothesis);
            createdHypothesis++;
        }

        int createdActions = 0;
        for (ActionDraft draft : analysis.actionPlans()) {
            AiActionPlan actionPlan = new AiActionPlan();
            actionPlan.setInvestigationId(investigationId);
            actionPlan.setUserId(userId);
            actionPlan.setActionType(nonBlank(upper(draft.actionType()), "RUNBOOK"));
            actionPlan.setTitle(nonBlank(draft.title(), "Action Plan"));
            actionPlan.setCommandText(draft.commandText());
            actionPlan.setRunbookRef(draft.runbookRef());
            actionPlan.setRiskLevel(normalizeRiskLevel(draft.riskLevel()));
            boolean requiresApproval = draft.requiresApproval() == null || draft.requiresApproval();
            actionPlan.setRequiresApproval(requiresApproval);
            actionPlan.setStatus(requiresApproval ? "PROPOSED" : "APPROVED");
            actionPlan.setRollbackPlan(draft.rollbackPlan());
            actionPlan.setCreatedAt(LocalDateTime.now());
            actionPlan.setUpdatedAt(LocalDateTime.now());
            aiActionPlanRepository.save(actionPlan);
            createdActions++;
        }

        int nextVersion = aiReportSnapshotRepository
                .findFirstByInvestigationIdAndUserIdOrderByVersionNoDesc(investigationId, userId)
                .map(snapshot -> snapshot.getVersionNo() + 1)
                .orElse(1);

        AiReportSnapshot snapshot = new AiReportSnapshot();
        snapshot.setInvestigationId(investigationId);
        snapshot.setUserId(userId);
        snapshot.setVersionNo(nextVersion);
        snapshot.setFormat("BOTH");
        snapshot.setReportMarkdown(analysis.reportMarkdown());
        snapshot.setReportJson(analysis.reportJson());
        snapshot.setCreatedBy(nonBlank(operator, "AI"));
        snapshot.setCreatedAt(LocalDateTime.now());
        AiReportSnapshot savedSnapshot = aiReportSnapshotRepository.save(snapshot);

        Integer postmortemVersion = null;
        if (includePostmortem) {
            AiReportSnapshot pm = createPostmortemSnapshot(
                    investigation,
                    userId,
                    nonBlank(operator, "AI"),
                    analysis.postmortemDraft(),
                    null
            );
            postmortemVersion = pm.getVersionNo();
        }

        eventPublisher.publish(
                userId,
                "AI_ANALYZED",
                investigationId,
                "AI structured analysis completed",
                Map.of(
                        "snapshotVersion", savedSnapshot.getVersionNo(),
                        "hypothesisCreated", createdHypothesis,
                        "actionCreated", createdActions
                )
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("snapshotVersion", savedSnapshot.getVersionNo());
        result.put("createdHypotheses", createdHypothesis);
        result.put("createdActionPlans", createdActions);
        result.put("riskLevel", analysis.riskLevel());
        result.put("postmortemVersion", postmortemVersion);
        return result;
    }

    @Transactional
    public Map<String, Object> generatePostmortemDraft(Long investigationId,
                                                       Long userId,
                                                       String operator,
                                                       String additionalContext) {
        AiInvestigation investigation = requireInvestigation(investigationId, userId);
        String draft = buildPostmortemDraftWithAi(investigation, additionalContext);
        AiReportSnapshot snapshot = createPostmortemSnapshot(
                investigation,
                userId,
                nonBlank(operator, "AI"),
                draft,
                additionalContext
        );

        eventPublisher.publish(
                userId,
                "POSTMORTEM_DRAFT",
                investigationId,
                "Postmortem draft generated",
                Map.of("snapshotVersion", snapshot.getVersionNo())
        );

        return Map.of(
                "snapshotId", snapshot.getId(),
                "versionNo", snapshot.getVersionNo(),
                "markdown", snapshot.getReportMarkdown()
        );
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> autoGenerateFromIncident(Long investigationId, Long userId) {
        try {
            generateAndPersist(investigationId, userId, "SYSTEM", "Auto generated from incident", false);
        } catch (Exception ex) {
            log.warn("auto structured analysis failed, investigationId={}, reason={}", investigationId, ex.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private StructuredAnalysis runAnalysis(AiInvestigation investigation,
                                           List<AiObservation> observations,
                                           String promptHint) {
        String prompt = buildStructuredPrompt(investigation, observations, promptHint);
        String raw = aiService.analyzeData(prompt);
        StructuredAnalysis parsed = parseStructuredAnalysis(raw);
        if (parsed != null) {
            return parsed;
        }
        return fallbackAnalysis(investigation, observations);
    }

    private StructuredAnalysis parseStructuredAnalysis(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String cleaned = extractJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);
            String riskLevel = normalizeRiskLevel(root.path("riskLevel").asText(null));
            String summary = nonBlank(root.path("summary").asText(null), "AI analysis generated.");
            String rootCause = nonBlank(root.path("rootCause").asText(null), summary);
            double confidence = clamp(root.path("confidence").asDouble(0.7d));
            List<HypothesisDraft> hypotheses = parseHypotheses(root.path("hypotheses"));
            List<ActionDraft> actions = parseActions(root.path("actionPlans"));
            String reportMarkdown = nonBlank(root.path("reportMarkdown").asText(null), buildReportMarkdown(summary, rootCause, riskLevel, hypotheses, actions));
            String postmortemDraft = nonBlank(root.path("postmortemDraft").asText(null), buildSimplePostmortem(summary, rootCause, actions));
            return new StructuredAnalysis(
                    summary,
                    rootCause,
                    confidence,
                    riskLevel,
                    hypotheses,
                    actions,
                    reportMarkdown,
                    objectMapper.writeValueAsString(root),
                    postmortemDraft
            );
        } catch (Exception ex) {
            log.warn("parse structured analysis failed: {}", ex.getMessage());
            return null;
        }
    }

    private StructuredAnalysis fallbackAnalysis(AiInvestigation investigation, List<AiObservation> observations) {
        AiObservation last = observations.isEmpty() ? null : observations.get(observations.size() - 1);
        String metric = last == null ? "UNKNOWN" : nonBlank(last.getMetricName(), nonBlank(last.getType(), "METRIC"));
        Double value = last == null ? null : last.getMetricValue();

        String summary = String.format("Potential abnormal signal on %s%s",
                metric,
                value == null ? "" : " value=" + value);
        String rootCause = "Resource pressure or workload spike needs validation.";
        String riskLevel = "MEDIUM";
        List<HypothesisDraft> hypotheses = List.of(
                new HypothesisDraft(
                        "Resource contention on host",
                        "Metric trend indicates sustained pressure near threshold.",
                        0.72,
                        "CANDIDATE"
                )
        );
        List<ActionDraft> actions = List.of(
                new ActionDraft(
                        "RUNBOOK",
                        "Collect top process snapshot",
                        "top -b -n 1 | head -40",
                        null,
                        "MEDIUM",
                        true,
                        "No-op"
                )
        );
        String reportMarkdown = buildReportMarkdown(summary, rootCause, riskLevel, hypotheses, actions);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("summary", summary);
        json.put("rootCause", rootCause);
        json.put("confidence", 0.72);
        json.put("riskLevel", riskLevel);
        json.put("hypotheses", hypotheses.stream().map(h -> Map.of(
                "title", h.title(),
                "reasoning", h.reasoning() == null ? "" : h.reasoning(),
                "confidence", h.confidence() == null ? 0.7d : h.confidence(),
                "status", h.status() == null ? "CANDIDATE" : h.status()
        )).toList());
        json.put("actionPlans", actions.stream().map(a -> Map.of(
                "actionType", a.actionType() == null ? "RUNBOOK" : a.actionType(),
                "title", a.title(),
                "commandText", a.commandText() == null ? "" : a.commandText(),
                "riskLevel", a.riskLevel() == null ? "MEDIUM" : a.riskLevel(),
                "requiresApproval", a.requiresApproval() == null || a.requiresApproval()
        )).toList());
        json.put("reportMarkdown", reportMarkdown);
        json.put("postmortemDraft", buildSimplePostmortem(summary, rootCause, actions));
        try {
            return new StructuredAnalysis(
                    summary,
                    rootCause,
                    0.72d,
                    riskLevel,
                    hypotheses,
                    actions,
                    reportMarkdown,
                    objectMapper.writeValueAsString(json),
                    buildSimplePostmortem(summary, rootCause, actions)
            );
        } catch (Exception ex) {
            throw new IllegalStateException("build fallback analysis failed", ex);
        }
    }

    private List<HypothesisDraft> parseHypotheses(JsonNode node) {
        List<HypothesisDraft> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode item : node) {
            String title = nonBlank(item.path("title").asText(null), null);
            if (title == null) {
                continue;
            }
            list.add(new HypothesisDraft(
                    title,
                    nonBlank(item.path("reasoning").asText(null), null),
                    clamp(item.path("confidence").asDouble(0.7)),
                    normalizeHypothesisStatus(item.path("status").asText(null))
            ));
        }
        return list;
    }

    private List<ActionDraft> parseActions(JsonNode node) {
        List<ActionDraft> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode item : node) {
            String title = nonBlank(item.path("title").asText(null), null);
            if (title == null) {
                continue;
            }
            list.add(new ActionDraft(
                    nonBlank(upper(item.path("actionType").asText(null)), "RUNBOOK"),
                    title,
                    nonBlank(item.path("commandText").asText(null), null),
                    nonBlank(item.path("runbookRef").asText(null), null),
                    normalizeRiskLevel(item.path("riskLevel").asText(null)),
                    item.path("requiresApproval").isMissingNode() ? Boolean.TRUE : item.path("requiresApproval").asBoolean(true),
                    nonBlank(item.path("rollbackPlan").asText(null), null)
            ));
        }
        return list;
    }

    private String buildStructuredPrompt(AiInvestigation investigation,
                                         List<AiObservation> observations,
                                         String promptHint) {
        StringBuilder obsBuilder = new StringBuilder();
        for (AiObservation obs : observations.stream().skip(Math.max(0, observations.size() - 12)).toList()) {
            obsBuilder.append("- ")
                    .append(nonBlank(obs.getType(), "METRIC"))
                    .append(" | metric=").append(nonBlank(obs.getMetricName(), "N/A"))
                    .append(" | value=").append(obs.getMetricValue() == null ? "-" : obs.getMetricValue())
                    .append(" | host=").append(nonBlank(obs.getHostname(), "unknown-host"))
                    .append(" | at=").append(obs.getObservedAt() == null ? obs.getCreatedAt() : obs.getObservedAt())
                    .append('\n');
        }
        if (obsBuilder.isEmpty()) {
            obsBuilder.append("- no observations");
        }

        return """
                你是企业级 AIOps 调查引擎。请输出严格 JSON（不要 markdown，不要解释文本）。
                JSON schema:
                {
                  "summary":"string",
                  "rootCause":"string",
                  "confidence":0.0,
                  "riskLevel":"LOW|MEDIUM|HIGH|CRITICAL",
                  "hypotheses":[{"title":"string","reasoning":"string","confidence":0.0,"status":"CANDIDATE|CONFIRMED|REJECTED"}],
                  "actionPlans":[{"actionType":"RUNBOOK|COMMAND","title":"string","commandText":"string","runbookRef":"string","riskLevel":"LOW|MEDIUM|HIGH","requiresApproval":true,"rollbackPlan":"string"}],
                  "reportMarkdown":"string",
                  "postmortemDraft":"string"
                }

                investigation:
                - id: %d
                - title: %s
                - severity: %s
                - summary: %s

                observations:
                %s

                hint:
                %s
                """.formatted(
                investigation.getId(),
                nonBlank(investigation.getTitle(), "N/A"),
                nonBlank(investigation.getSeverity(), "P2"),
                nonBlank(investigation.getSummary(), "N/A"),
                obsBuilder,
                nonBlank(promptHint, "no extra hint")
        );
    }

    private String buildPostmortemDraftWithAi(AiInvestigation investigation, String additionalContext) {
        List<AiHypothesis> hypotheses = aiHypothesisRepository
                .findByInvestigationIdAndUserIdOrderByRankOrderAsc(investigation.getId(), investigation.getUserId());
        List<AiActionPlan> actionPlans = aiActionPlanRepository
                .findByInvestigationIdAndUserIdOrderByCreatedAtAsc(investigation.getId(), investigation.getUserId());
        String prompt = """
                请产出一份运维复盘草稿（Markdown），包含以下章节：
                1. 事件概述
                2. 时间线
                3. 根因分析
                4. 处置动作与效果
                5. 改进项（短期/长期）
                6. 预防策略

                investigation:
                - id: %d
                - title: %s
                - severity: %s
                - startedAt: %s
                - closedAt: %s
                - summary: %s

                hypotheses:
                %s

                actionPlans:
                %s

                extra:
                %s
                """.formatted(
                investigation.getId(),
                nonBlank(investigation.getTitle(), "N/A"),
                nonBlank(investigation.getSeverity(), "P2"),
                investigation.getStartedAt(),
                investigation.getClosedAt(),
                nonBlank(investigation.getSummary(), "N/A"),
                hypotheses.isEmpty() ? "none" : hypotheses.toString(),
                actionPlans.isEmpty() ? "none" : actionPlans.toString(),
                nonBlank(additionalContext, "none")
        );

        String markdown = aiService.analyzeData(prompt);
        if (markdown == null || markdown.isBlank()) {
            return buildSimplePostmortem(
                    nonBlank(investigation.getSummary(), "No summary"),
                    nonBlank(investigation.getRootCause(), "Need further validation"),
                    List.of()
            );
        }
        return markdown;
    }

    private AiReportSnapshot createPostmortemSnapshot(AiInvestigation investigation,
                                                      Long userId,
                                                      String operator,
                                                      String markdown,
                                                      String additionalContext) {
        int nextVersion = aiReportSnapshotRepository
                .findFirstByInvestigationIdAndUserIdOrderByVersionNoDesc(investigation.getId(), userId)
                .map(snapshot -> snapshot.getVersionNo() + 1)
                .orElse(1);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("kind", "POSTMORTEM_DRAFT");
        meta.put("generatedAt", LocalDateTime.now());
        if (additionalContext != null && !additionalContext.isBlank()) {
            meta.put("additionalContext", additionalContext);
        }

        AiReportSnapshot snapshot = new AiReportSnapshot();
        snapshot.setInvestigationId(investigation.getId());
        snapshot.setUserId(userId);
        snapshot.setVersionNo(nextVersion);
        snapshot.setFormat("BOTH");
        snapshot.setReportMarkdown(markdown);
        try {
            snapshot.setReportJson(objectMapper.writeValueAsString(meta));
        } catch (Exception ex) {
            snapshot.setReportJson("{\"kind\":\"POSTMORTEM_DRAFT\"}");
        }
        snapshot.setCreatedBy(operator);
        snapshot.setCreatedAt(LocalDateTime.now());
        return aiReportSnapshotRepository.save(snapshot);
    }

    private AiInvestigation requireInvestigation(Long id, Long userId) {
        return aiInvestigationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "调查不存在"));
    }

    private String extractJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replace("```json", "").replace("```", "").trim();
        }
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return text.substring(first, last + 1);
        }
        return text;
    }

    private String buildReportMarkdown(String summary,
                                       String rootCause,
                                       String riskLevel,
                                       List<HypothesisDraft> hypotheses,
                                       List<ActionDraft> actions) {
        StringBuilder md = new StringBuilder();
        md.append("## AI Investigation Summary\n")
                .append("- Risk Level: ").append(riskLevel).append('\n')
                .append("- Summary: ").append(summary).append('\n')
                .append("- Root Cause: ").append(rootCause).append("\n\n")
                .append("### Hypotheses\n");
        if (hypotheses.isEmpty()) {
            md.append("- None\n");
        } else {
            for (HypothesisDraft h : hypotheses) {
                md.append("- ").append(nonBlank(h.title(), "N/A"))
                        .append(" (").append(nonBlank(h.status(), "CANDIDATE")).append(")")
                        .append(" confidence=").append(h.confidence() == null ? "-" : h.confidence())
                        .append('\n');
            }
        }
        md.append("\n### Action Plans\n");
        if (actions.isEmpty()) {
            md.append("- None\n");
        } else {
            for (ActionDraft a : actions) {
                md.append("- ").append(nonBlank(a.title(), "N/A"))
                        .append(" [").append(nonBlank(a.riskLevel(), "MEDIUM")).append("]");
                if (a.commandText() != null) {
                    md.append("\n  - cmd: `").append(a.commandText()).append('`');
                }
                md.append('\n');
            }
        }
        return md.toString();
    }

    private String buildSimplePostmortem(String summary, String rootCause, List<ActionDraft> actions) {
        StringBuilder md = new StringBuilder();
        md.append("## Postmortem Draft\n")
                .append("### 1. Incident Summary\n")
                .append(summary).append("\n\n")
                .append("### 2. Root Cause\n")
                .append(rootCause).append("\n\n")
                .append("### 3. Actions Taken\n");
        if (actions == null || actions.isEmpty()) {
            md.append("- Pending validation\n");
        } else {
            for (ActionDraft a : actions) {
                md.append("- ").append(nonBlank(a.title(), "N/A")).append('\n');
            }
        }
        md.append("\n### 4. Preventive Improvements\n")
                .append("- Add alert calibration review.\n")
                .append("- Add runbook verification in CI.\n");
        return md.toString();
    }

    private String mapRiskToSeverity(String riskLevel) {
        String normalized = normalizeRiskLevel(riskLevel);
        if ("HIGH".equals(normalized) || "CRITICAL".equals(normalized)) {
            return "P1";
        }
        if ("MEDIUM".equals(normalized)) {
            return "P2";
        }
        return "P3";
    }

    private String normalizeHypothesisStatus(String status) {
        String normalized = upper(status);
        if ("CONFIRMED".equals(normalized) || "REJECTED".equals(normalized)) {
            return normalized;
        }
        return "CANDIDATE";
    }

    private String normalizeRiskLevel(String riskLevel) {
        String normalized = upper(riskLevel);
        if ("LOW".equals(normalized) || "MEDIUM".equals(normalized) || "HIGH".equals(normalized) || "CRITICAL".equals(normalized)) {
            return normalized;
        }
        return "MEDIUM";
    }

    private String upper(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private double clamp(double value) {
        if (value < 0d) return 0d;
        return Math.min(value, 1d);
    }

    private record StructuredAnalysis(
            String summary,
            String rootCause,
            Double confidence,
            String riskLevel,
            List<HypothesisDraft> hypotheses,
            List<ActionDraft> actionPlans,
            String reportMarkdown,
            String reportJson,
            String postmortemDraft
    ) {
    }

    private record HypothesisDraft(
            String title,
            String reasoning,
            Double confidence,
            String status
    ) {
    }

    private record ActionDraft(
            String actionType,
            String title,
            String commandText,
            String runbookRef,
            String riskLevel,
            Boolean requiresApproval,
            String rollbackPlan
    ) {
    }
}
