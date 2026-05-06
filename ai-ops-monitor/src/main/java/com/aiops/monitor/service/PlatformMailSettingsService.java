package com.aiops.monitor.service;

import com.aiops.monitor.model.dto.PlatformMailSettingsRequest;
import com.aiops.monitor.model.entity.PlatformMailSettings;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.PlatformMailSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlatformMailSettingsService {

    private final PlatformMailSettingsRepository repository;

    public PlatformMailSettings getCurrent() {
        return repository.findFirstByOrderByIdAsc().orElse(null);
    }

    public PlatformMailSettings save(PlatformMailSettingsRequest request, User actor) {
        PlatformMailSettings settings = repository.findFirstByOrderByIdAsc().orElseGet(PlatformMailSettings::new);
        settings.setSmtpHost(normalize(request.getSmtpHost(), "smtpHost"));
        settings.setSmtpPort(request.getSmtpPort() == null ? 587 : request.getSmtpPort());
        settings.setSmtpUsername(normalize(request.getSmtpUsername(), "smtpUsername"));
        if (settings.getId() == null && isBlank(request.getSmtpPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "首次配置必须填写 smtpPassword");
        }
        if (!isBlank(request.getSmtpPassword())) {
            settings.setSmtpPassword(request.getSmtpPassword().trim());
        }
        settings.setSmtpAuth(request.getSmtpAuth() == null || request.getSmtpAuth());
        settings.setSmtpStarttls(request.getSmtpStarttls() == null || request.getSmtpStarttls());
        settings.setFromAddress(normalize(request.getFromAddress(), "fromAddress"));
        settings.setFromName(normalizeNullable(request.getFromName()));
        settings.setEnabled(request.getEnabled() == null || request.getEnabled());
        settings.setUpdatedAt(LocalDateTime.now());
        if (settings.getCreatedAt() == null) {
            settings.setCreatedAt(LocalDateTime.now());
        }
        settings.setUpdatedBy(actor == null ? null : actor.getUsername());
        return repository.save(settings);
    }

    private String normalize(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " 不能为空");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
