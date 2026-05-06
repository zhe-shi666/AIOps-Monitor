package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.PlatformMailSettingsRequest;
import com.aiops.monitor.model.dto.PlatformMailTestRequest;
import com.aiops.monitor.model.entity.PlatformMailSettings;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.service.AgentReleaseService;
import com.aiops.monitor.service.AuditLogService;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.NotificationEmailService;
import com.aiops.monitor.service.PlatformMailSettingsService;
import com.aiops.monitor.service.RoleGuardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformConfigController {
    private final AgentReleaseService agentReleaseService;
    private final PlatformMailSettingsService platformMailSettingsService;
    private final NotificationEmailService notificationEmailService;
    private final CurrentUserService currentUserService;
    private final RoleGuardService roleGuardService;
    private final AuditLogService auditLogService;

    @GetMapping("/agent-release")
    public ResponseEntity<Map<String, Object>> agentRelease() {
        return ResponseEntity.ok(agentReleaseService.currentRelease());
    }

    @GetMapping("/mail-settings")
    public ResponseEntity<Map<String, Object>> mailSettings(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        roleGuardService.requireOperator(user);
        PlatformMailSettings settings = platformMailSettingsService.getCurrent();
        return ResponseEntity.ok(toView(settings));
    }

    @PutMapping("/mail-settings")
    public ResponseEntity<Map<String, Object>> updateMailSettings(@Valid @RequestBody PlatformMailSettingsRequest request,
                                                                  Authentication authentication,
                                                                  HttpServletRequest httpRequest) {
        User user = currentUserService.requireUser(authentication);
        if (user.getRole() != User.Role.ADMIN) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "仅管理员可修改平台发信配置");
        }
        PlatformMailSettings settings = platformMailSettingsService.save(request, user);
        auditLogService.record(authentication, httpRequest, "PLATFORM_MAIL_SETTINGS_UPDATE", "PLATFORM_MAIL_SETTINGS",
                settings.getId(), Map.of("enabled", settings.isEnabled(), "smtpHost", settings.getSmtpHost(), "fromAddress", settings.getFromAddress()));
        return ResponseEntity.ok(toView(settings));
    }

    @PostMapping("/mail-settings/test")
    public ResponseEntity<Map<String, Object>> testMailSettings(@Valid @RequestBody PlatformMailTestRequest request,
                                                                Authentication authentication,
                                                                HttpServletRequest httpRequest) {
        User user = currentUserService.requireUser(authentication);
        if (user.getRole() != User.Role.ADMIN) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "仅管理员可测试平台发信配置");
        }
        notificationEmailService.sendPlatformTestMail(request.getRecipientEmail());
        auditLogService.record(authentication, httpRequest, "PLATFORM_MAIL_SETTINGS_TEST", "PLATFORM_MAIL_SETTINGS",
                null, Map.of("recipientEmail", request.getRecipientEmail()));
        return ResponseEntity.ok(Map.of("message", "测试邮件已发送"));
    }

    private Map<String, Object> toView(PlatformMailSettings settings) {
        Map<String, Object> view = new LinkedHashMap<>();
        if (settings == null) {
            view.put("configured", false);
            view.put("enabled", false);
            return view;
        }
        view.put("configured", true);
        view.put("id", settings.getId());
        view.put("smtpHost", settings.getSmtpHost());
        view.put("smtpPort", settings.getSmtpPort());
        view.put("smtpUsername", settings.getSmtpUsername());
        view.put("smtpPasswordConfigured", settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank());
        view.put("smtpAuth", settings.isSmtpAuth());
        view.put("smtpStarttls", settings.isSmtpStarttls());
        view.put("fromAddress", settings.getFromAddress());
        view.put("fromName", settings.getFromName());
        view.put("enabled", settings.isEnabled());
        view.put("updatedBy", settings.getUpdatedBy());
        view.put("updatedAt", settings.getUpdatedAt());
        return view;
    }
}
