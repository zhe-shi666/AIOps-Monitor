package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.EscalationPolicyUpdateRequest;
import com.aiops.monitor.model.entity.AlertEscalationPolicy;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.EscalationPolicyService;
import com.aiops.monitor.service.RoleGuardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/escalation-policy")
@RequiredArgsConstructor
public class EscalationPolicyController {

    private final EscalationPolicyService escalationPolicyService;
    private final CurrentUserService currentUserService;
    private final RoleGuardService roleGuardService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPolicy(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        AlertEscalationPolicy policy = escalationPolicyService.getOrCreateByUserId(user.getId());
        return ResponseEntity.ok(toView(policy));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updatePolicy(@Valid @RequestBody EscalationPolicyUpdateRequest request,
                                                            Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireOperator(user);
        AlertEscalationPolicy policy = escalationPolicyService.update(user.getId(), request);
        return ResponseEntity.ok(toView(policy));
    }

    private Map<String, Object> toView(AlertEscalationPolicy policy) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("p1Intervals", policy.getP1Intervals());
        view.put("p2Intervals", policy.getP2Intervals());
        view.put("p3Intervals", policy.getP3Intervals());
        view.put("updatedAt", policy.getUpdatedAt());
        return view;
    }
}
