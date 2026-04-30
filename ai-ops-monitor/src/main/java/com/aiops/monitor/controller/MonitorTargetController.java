package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.TargetCreateRequest;
import com.aiops.monitor.model.dto.TargetThresholdUpdateRequest;
import com.aiops.monitor.model.dto.TargetUpdateRequest;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.UserRepository;
import com.aiops.monitor.service.AgentKeyGenerator;
import com.aiops.monitor.service.AuditLogService;
import com.aiops.monitor.service.RoleGuardService;
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

    @Value("${monitor.agent.latest-version:agent-lite-1.2.0-cross}")
    private String latestAgentVersion;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        List<Map<String, Object>> targets = monitorTargetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toView)
                .toList();
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id, Authentication authentication) {
        Long userId = currentUserId(authentication);
        MonitorTarget target = requireTarget(id, userId);
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
        MonitorTarget target = requireTarget(id, user.getId());

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
        Long userId = currentUserId(authentication);
        requireTarget(id, userId);
        return ResponseEntity.ok(targetThresholdService.toView(userId, id));
    }

    @PutMapping("/{id}/thresholds")
    public ResponseEntity<Map<String, Object>> updateTargetThresholds(@PathVariable Long id,
                                                                      @Valid @RequestBody TargetThresholdUpdateRequest request,
                                                                      Authentication authentication,
                                                                      HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        roleGuardService.requireOperator(user);
        MonitorTarget target = requireTarget(id, user.getId());
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
        MonitorTarget target = requireTarget(id, user.getId());
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
        MonitorTarget target = requireTarget(id, user.getId());
        monitorTargetRepository.delete(target);
        auditLogService.record(authentication, httpRequest, "TARGET_DELETE", "MONITOR_TARGET", id, Map.of("name", target.getName()));
        return ResponseEntity.ok(Map.of("message", "监控目标已删除"));
    }

    private MonitorTarget requireTarget(Long id, Long userId) {
        return monitorTargetRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "监控目标不存在"));
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
        result.put("createdAt", target.getCreatedAt());
        result.put("lastHeartbeatAt", target.getLastHeartbeatAt());
        return result;
    }
}
