package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.ThresholdUpdateRequest;
import com.aiops.monitor.model.entity.AlertThresholdConfig;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.RoleGuardService;
import com.aiops.monitor.service.ThresholdConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings/thresholds")
@RequiredArgsConstructor
public class ThresholdConfigController {

    private final ThresholdConfigService thresholdConfigService;
    private final CurrentUserService currentUserService;
    private final RoleGuardService roleGuardService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getThresholds(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        AlertThresholdConfig config = thresholdConfigService.getOrCreateByUserId(user.getId());
        return ResponseEntity.ok(toView(config));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateThresholds(@Valid @RequestBody ThresholdUpdateRequest request,
                                                                Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireOperator(user);
        AlertThresholdConfig config = thresholdConfigService.update(
                user.getId(),
                request.getCpuThreshold(),
                request.getMemoryThreshold(),
                request.getDiskThreshold(),
                request.getProcessCountThreshold(),
                request.getConsecutiveBreachCount(),
                request.getSilenceSeconds()
        );
        return ResponseEntity.ok(toView(config));
    }

    private Map<String, Object> toView(AlertThresholdConfig config) {
        return Map.of(
                "cpuThreshold", config.getCpuThreshold(),
                "memoryThreshold", config.getMemoryThreshold(),
                "diskThreshold", config.getDiskThreshold(),
                "processCountThreshold", config.getProcessCountThreshold(),
                "consecutiveBreachCount", config.getConsecutiveBreachCount(),
                "silenceSeconds", config.getSilenceSeconds(),
                "updatedAt", config.getUpdatedAt()
        );
    }
}
