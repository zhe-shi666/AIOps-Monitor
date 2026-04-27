package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.NotificationChannelEnabledRequest;
import com.aiops.monitor.model.dto.NotificationChannelRequest;
import com.aiops.monitor.model.entity.NotificationChannel;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.NotificationChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/notification-channels")
@RequiredArgsConstructor
public class NotificationChannelController {

    private final NotificationChannelService notificationChannelService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        List<Map<String, Object>> data = notificationChannelService.listByUserId(user.getId())
                .stream()
                .map(this::toView)
                .toList();
        return ResponseEntity.ok(data);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody NotificationChannelRequest request,
                                                      Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        NotificationChannel channel = notificationChannelService.create(user.getId(), request);
        return ResponseEntity.ok(toView(channel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                      @Valid @RequestBody NotificationChannelRequest request,
                                                      Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        NotificationChannel channel = notificationChannelService.update(user.getId(), id, request);
        return ResponseEntity.ok(toView(channel));
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<Map<String, Object>> setEnabled(@PathVariable Long id,
                                                           @Valid @RequestBody NotificationChannelEnabledRequest request,
                                                           Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        NotificationChannel channel = notificationChannelService.setEnabled(user.getId(), id, request.getEnabled());
        return ResponseEntity.ok(toView(channel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        notificationChannelService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toView(NotificationChannel channel) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", channel.getId());
        view.put("name", channel.getName());
        view.put("type", channel.getType());
        view.put("webhookUrl", channel.getWebhookUrl());
        view.put("enabled", channel.isEnabled());
        view.put("secretConfigured", channel.getSecret() != null && !channel.getSecret().isBlank());
        view.put("lastNotifiedAt", channel.getLastNotifiedAt());
        view.put("lastError", channel.getLastError());
        view.put("updatedAt", channel.getUpdatedAt());
        return view;
    }
}
