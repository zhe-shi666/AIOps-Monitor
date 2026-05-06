package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.TargetCreateRequest;
import com.aiops.monitor.model.dto.TargetMemberAssignRequest;
import com.aiops.monitor.model.dto.TargetSubscriptionUpdateRequest;
import com.aiops.monitor.model.dto.TargetThresholdUpdateRequest;
import com.aiops.monitor.model.dto.TargetUpdateRequest;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.UserRepository;
import com.aiops.monitor.service.AgentKeyGenerator;
import com.aiops.monitor.service.AuditLogService;
import com.aiops.monitor.service.RoleGuardService;
import com.aiops.monitor.service.TargetNotificationSubscriptionService;
import com.aiops.monitor.service.TargetThresholdService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class MonitorTargetController {

    private final MonitorTargetRepository monitorTargetRepository;
    private final UserRepository userRepository;
    private final AgentKeyGenerator agentKeyGenerator;
    private final TargetThresholdService targetThresholdService;
    private final RoleGuardService roleGuardService;
    private final AuditLogService auditLogService;
    private final TargetNotificationSubscriptionService targetNotificationSubscriptionService;

    @Value("${monitor.agent.latest-version:agent-lite-1.2.0-cross}")
    private String latestAgentVersion;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication) {
        User user = currentUser(authentication);
        List<Map<String, Object>> targets;
        if (isOperator(user)) {
            targets = monitorTargetRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .map(this::toView)
                    .toList();
        } else {
            java.util.Set<Long> subscribedTargetIds = targetNotificationSubscriptionService.listSubscribedTargetIds(user.getId());
            targets = monitorTargetRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .filter(target -> subscribedTargetIds.contains(target.getId()))
                    .map(this::toView)
                    .toList();
        }
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        MonitorTarget target = requireViewableTarget(id, user);
        return ResponseEntity.ok(toView(target));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody TargetCreateRequest request,
                                                      Authentication authentication,
                                                      HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = new MonitorTarget();
        target.setUserId(user.getId());
        target.setName(request.getName().trim());
        target.setHostname(blankToNull(request.getHostname()));
        target.setDescription(blankToNull(request.getDescription()));
        target.setAgentKey(agentKeyGenerator.generateUniqueKey());
        target.setCreatedAt(LocalDateTime.now());
        target.setStatus("OFFLINE");
        target.setEnabled(true);
        monitorTargetRepository.save(target);
        targetNotificationSubscriptionService.ensureSubscribed(target.getId(), user.getId());
        auditLogService.record(authentication, httpRequest, "TARGET_CREATE", "MONITOR_TARGET", target.getId(), Map.of("name", target.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(target));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @Valid @RequestBody TargetUpdateRequest request,
                                                      Authentication authentication,
                                                      HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            target.setName(request.getName().trim());
        }
        if (request.getHostname() != null) {
            target.setHostname(blankToNull(request.getHostname()));
        }
        if (request.getDescription() != null) {
            target.setDescription(blankToNull(request.getDescription()));
        }
        if (request.getEnabled() != null) {
            target.setEnabled(request.getEnabled());
            if (!request.getEnabled()) {
                target.setStatus("DISABLED");
            } else if (!"ONLINE".equalsIgnoreCase(target.getStatus())) {
                target.setStatus("OFFLINE");
            }
        }

        monitorTargetRepository.save(target);
        auditLogService.record(authentication, httpRequest, "TARGET_UPDATE", "MONITOR_TARGET", target.getId(), Map.of("name", target.getName()));
        return ResponseEntity.ok(toView(target));
    }

    @GetMapping("/{id}/thresholds")
    public ResponseEntity<Map<String, Object>> getTargetThresholds(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        requireViewableTarget(id, user);
        return ResponseEntity.ok(targetThresholdService.toView(user.getId(), id));
    }

    @PutMapping("/{id}/thresholds")
    public ResponseEntity<Map<String, Object>> updateTargetThresholds(@PathVariable Long id,
                                                                      @Valid @RequestBody TargetThresholdUpdateRequest request,
                                                                      Authentication authentication,
                                                                      HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id);
        targetThresholdService.update(user.getId(), id, request);
        auditLogService.record(authentication, httpRequest, "TARGET_THRESHOLD_UPDATE", "MONITOR_TARGET", id, Map.of(
                "targetId", id,
                "name", target.getName()
        ));
        return ResponseEntity.ok(targetThresholdService.toView(user.getId(), id));
    }

    @PostMapping("/{id}/rotate-key")
    public ResponseEntity<Map<String, Object>> rotateAgentKey(@PathVariable Long id,
                                                              Authentication authentication,
                                                              HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id);
        target.setAgentKey(agentKeyGenerator.generateUniqueKey());
        monitorTargetRepository.save(target);
        auditLogService.record(authentication, httpRequest, "TARGET_ROTATE_KEY", "MONITOR_TARGET", target.getId(), Map.of("name", target.getName()));
        return ResponseEntity.ok(Map.of(
                "id", target.getId(),
                "name", target.getName(),
                "agentKey", target.getAgentKey(),
                "message", "Agent Key 已轮换，请在 Agent 端更新配置"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      Authentication authentication,
                                                      HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id);
        monitorTargetRepository.delete(target);
        auditLogService.record(authentication, httpRequest, "TARGET_DELETE", "MONITOR_TARGET", id, Map.of("name", target.getName()));
        return ResponseEntity.ok(Map.of("message", "监控目标已删除"));
    }

    @GetMapping("/{id}/subscription")
    public ResponseEntity<Map<String, Object>> getMySubscription(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        MonitorTarget target = requireViewableTarget(id, user);
        return ResponseEntity.ok(Map.of(
                "targetId", target.getId(),
                "enabled", targetNotificationSubscriptionService.isSubscribed(target.getId(), user.getId()),
                "subscriberCount", targetNotificationSubscriptionService.countSubscribers(target.getId())
        ));
    }

    @PutMapping("/{id}/subscription")
    public ResponseEntity<Map<String, Object>> updateMySubscription(@PathVariable Long id,
                                                                    @Valid @RequestBody TargetSubscriptionUpdateRequest request,
                                                                    Authentication authentication,
                                                                    HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id);
        targetNotificationSubscriptionService.setSubscription(target.getId(), user.getId(), Boolean.TRUE.equals(request.getEnabled()));
        auditLogService.record(authentication, httpRequest, "TARGET_NOTIFICATION_SUBSCRIPTION_UPDATE", "MONITOR_TARGET", id, Map.of(
                "enabled", Boolean.TRUE.equals(request.getEnabled()),
                "targetName", target.getName()
        ));
        return ResponseEntity.ok(Map.of(
                "targetId", target.getId(),
                "enabled", targetNotificationSubscriptionService.isSubscribed(target.getId(), user.getId()),
                "subscriberCount", targetNotificationSubscriptionService.countSubscribers(target.getId())
        ));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<Map<String, Object>>> listMembers(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id);
        List<Map<String, Object>> result = targetNotificationSubscriptionService.listSubscriberUsers(target.getId())
                .stream()
                .map(member -> Map.<String, Object>of(
                        "id", member.getId(),
                        "username", member.getUsername(),
                        "email", member.getEmail(),
                        "notificationEmail", member.getNotificationEmail() == null ? "" : member.getNotificationEmail(),
                        "notificationEnabled", member.isNotificationEnabled(),
                        "role", member.getRole().name(),
                        "enabled", member.isEnabled()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/members")
    public ResponseEntity<Map<String, Object>> updateMember(@PathVariable Long id,
                                                            @Valid @RequestBody TargetMemberAssignRequest request,
                                                            Authentication authentication,
                                                            HttpServletRequest httpRequest) {
        User actor = currentUser(authentication);
        roleGuardService.requireOperator(actor);
        MonitorTarget target = requireTarget(id);
        User member = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        targetNotificationSubscriptionService.setSubscription(target.getId(), member.getId(), Boolean.TRUE.equals(request.getEnabled()));
        auditLogService.record(authentication, httpRequest, "TARGET_MEMBER_ASSIGN_UPDATE", "MONITOR_TARGET", id, Map.of(
                "memberUserId", member.getId(),
                "memberUsername", member.getUsername(),
                "enabled", Boolean.TRUE.equals(request.getEnabled())
        ));
        return ResponseEntity.ok(Map.of(
                "targetId", target.getId(),
                "userId", member.getId(),
                "enabled", targetNotificationSubscriptionService.isSubscribed(target.getId(), member.getId()),
                "subscriberCount", targetNotificationSubscriptionService.countSubscribers(target.getId())
        ));
    }

    private MonitorTarget requireTarget(Long id) {
        return monitorTargetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "监控目标不存在"));
    }

    private MonitorTarget requireSubscribedTarget(Long id, User user) {
        MonitorTarget target = requireTarget(id);
        if (!targetNotificationSubscriptionService.isSubscribed(target.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "监控目标不存在");
        }
        return target;
    }

    private MonitorTarget requireViewableTarget(Long id, User user) {
        if (isOperator(user)) {
            return requireTarget(id);
        }
        return requireSubscribedTarget(id, user);
    }

    private boolean isOperator(User user) {
        return user != null
                && user.getRole() != null
                && (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.OPS);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    private Long currentUserId(Authentication authentication) {
        return currentUser(authentication).getId();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> toView(MonitorTarget target) {
        String agentKey = target.getAgentKey() == null ? "" : target.getAgentKey();
        String preview = agentKey.length() >= 8 ? agentKey.substring(0, 8) + "****" : "****";
        Map<String, Object> result = new HashMap<>();
        result.put("id", target.getId());
        result.put("name", target.getName());
        result.put("hostname", target.getHostname() == null ? "" : target.getHostname());
        result.put("description", target.getDescription() == null ? "" : target.getDescription());
        result.put("agentKey", agentKey);
        result.put("agentKeyPreview", preview);
        result.put("enabled", target.isEnabled());
        result.put("status", target.getStatus() == null ? "OFFLINE" : target.getStatus());
        result.put("ipAddress", target.getIpAddress() == null ? "" : target.getIpAddress());
        result.put("agentVersion", target.getAgentVersion() == null ? "" : target.getAgentVersion());
        result.put("latestAgentVersion", latestAgentVersion);
        result.put("agentOutdated", target.getAgentVersion() != null && !target.getAgentVersion().isBlank()
                && !latestAgentVersion.equalsIgnoreCase(target.getAgentVersion()));
        result.put("subscriberCount", targetNotificationSubscriptionService.countSubscribers(target.getId()));
        result.put("createdAt", target.getCreatedAt());
        result.put("lastHeartbeatAt", target.getLastHeartbeatAt());
        return result;
    }
}
