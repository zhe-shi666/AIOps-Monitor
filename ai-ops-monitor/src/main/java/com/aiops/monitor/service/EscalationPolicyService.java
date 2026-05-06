package com.aiops.monitor.service;

import com.aiops.monitor.model.dto.EscalationPolicyUpdateRequest;
import com.aiops.monitor.model.entity.AlertEscalationPolicy;
import com.aiops.monitor.model.entity.TargetNotificationSubscription;
import com.aiops.monitor.repository.TargetNotificationSubscriptionRepository;
import com.aiops.monitor.repository.AlertEscalationPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EscalationPolicyService {

    private final AlertEscalationPolicyRepository alertEscalationPolicyRepository;
    private final TargetNotificationSubscriptionRepository targetNotificationSubscriptionRepository;

    @Value("${monitor.escalation.p1-intervals:1,3,5,10}")
    private String defaultP1Intervals;

    @Value("${monitor.escalation.p2-intervals:5,15,30}")
    private String defaultP2Intervals;

    @Value("${monitor.escalation.p3-intervals:15,30,60}")
    private String defaultP3Intervals;

    public AlertEscalationPolicy getOrCreateByUserId(Long userId) {
        AlertEscalationPolicy policy = alertEscalationPolicyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    AlertEscalationPolicy created = new AlertEscalationPolicy();
                    created.setUserId(userId);
                    created.setP1Intervals(normalizeIntervals(defaultP1Intervals, "p1Intervals"));
                    created.setP2Intervals(normalizeIntervals(defaultP2Intervals, "p2Intervals"));
                    created.setP3Intervals(normalizeIntervals(defaultP3Intervals, "p3Intervals"));
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return alertEscalationPolicyRepository.save(created);
                });

        boolean changed = false;
        if (policy.getP1Intervals() == null || policy.getP1Intervals().isBlank()) {
            policy.setP1Intervals(normalizeIntervals(defaultP1Intervals, "p1Intervals"));
            changed = true;
        }
        if (policy.getP2Intervals() == null || policy.getP2Intervals().isBlank()) {
            policy.setP2Intervals(normalizeIntervals(defaultP2Intervals, "p2Intervals"));
            changed = true;
        }
        if (policy.getP3Intervals() == null || policy.getP3Intervals().isBlank()) {
            policy.setP3Intervals(normalizeIntervals(defaultP3Intervals, "p3Intervals"));
            changed = true;
        }
        if (changed) {
            policy.setUpdatedAt(LocalDateTime.now());
            return alertEscalationPolicyRepository.save(policy);
        }
        return policy;
    }

    public AlertEscalationPolicy update(Long userId, EscalationPolicyUpdateRequest request) {
        AlertEscalationPolicy policy = getOrCreateByUserId(userId);
        policy.setP1Intervals(normalizeIntervals(request.getP1Intervals(), "p1Intervals"));
        policy.setP2Intervals(normalizeIntervals(request.getP2Intervals(), "p2Intervals"));
        policy.setP3Intervals(normalizeIntervals(request.getP3Intervals(), "p3Intervals"));
        policy.setUpdatedAt(LocalDateTime.now());
        return alertEscalationPolicyRepository.save(policy);
    }

    public AlertEscalationPolicy resolvePolicyForTarget(Long targetId) {
        if (targetId == null) {
            return null;
        }
        List<TargetNotificationSubscription> subscriptions = targetNotificationSubscriptionRepository.findByTargetIdAndEnabledTrue(targetId);
        for (TargetNotificationSubscription subscription : subscriptions) {
            if (subscription.getUserId() == null) {
                continue;
            }
            return getOrCreateByUserId(subscription.getUserId());
        }
        return null;
    }

    public Integer getIntervalMinutes(AlertEscalationPolicy policy, String severity, int index) {
        if (policy == null || index < 0) {
            return null;
        }
        List<Integer> intervals = intervalsBySeverity(policy, severity);
        if (index >= intervals.size()) {
            return null;
        }
        return intervals.get(index);
    }

    public List<Integer> intervalsBySeverity(AlertEscalationPolicy policy, String severity) {
        String normalizedSeverity = (severity == null ? "P2" : severity.trim().toUpperCase(Locale.ROOT));
        return switch (normalizedSeverity) {
            case "P1" -> parseIntervals(policy.getP1Intervals(), "p1Intervals");
            case "P3" -> parseIntervals(policy.getP3Intervals(), "p3Intervals");
            default -> parseIntervals(policy.getP2Intervals(), "p2Intervals");
        };
    }

    private String normalizeIntervals(String source, String fieldName) {
        List<Integer> intervals = parseIntervals(source, fieldName);
        return intervals.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Integer> parseIntervals(String source, String fieldName) {
        if (source == null || source.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " 不能为空");
        }

        String compact = source.replace(" ", "").replace("，", ",").trim();
        List<String> parts = Arrays.stream(compact.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (parts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " 格式不合法");
        }

        List<Integer> intervals = new ArrayList<>();
        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 1 || value > 1440) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            fieldName + " 单个值必须在 1~1440 分钟");
                }
                intervals.add(value);
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        fieldName + " 仅支持逗号分隔整数，例如 1,3,5");
            }
        }

        return intervals;
    }
}
