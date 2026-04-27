package com.aiops.monitor.service;

import com.aiops.monitor.model.dto.NotificationChannelRequest;
import com.aiops.monitor.model.entity.NotificationChannel;
import com.aiops.monitor.repository.NotificationChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationChannelService {

    private final NotificationChannelRepository notificationChannelRepository;

    public List<NotificationChannel> listByUserId(Long userId) {
        return notificationChannelRepository.findByUserIdOrderByIdDesc(userId);
    }

    public NotificationChannel create(Long userId, NotificationChannelRequest request) {
        NotificationChannel channel = new NotificationChannel();
        channel.setUserId(userId);
        apply(request, channel, false);
        channel.setCreatedAt(LocalDateTime.now());
        channel.setUpdatedAt(LocalDateTime.now());
        return notificationChannelRepository.save(channel);
    }

    public NotificationChannel update(Long userId, Long id, NotificationChannelRequest request) {
        NotificationChannel channel = requireByIdAndUserId(id, userId);
        apply(request, channel, true);
        channel.setUpdatedAt(LocalDateTime.now());
        return notificationChannelRepository.save(channel);
    }

    public NotificationChannel setEnabled(Long userId, Long id, boolean enabled) {
        NotificationChannel channel = requireByIdAndUserId(id, userId);
        channel.setEnabled(enabled);
        channel.setUpdatedAt(LocalDateTime.now());
        return notificationChannelRepository.save(channel);
    }

    public void delete(Long userId, Long id) {
        NotificationChannel channel = requireByIdAndUserId(id, userId);
        notificationChannelRepository.delete(channel);
    }

    private NotificationChannel requireByIdAndUserId(Long id, Long userId) {
        return notificationChannelRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "通知通道不存在"));
    }

    private void apply(NotificationChannelRequest request, NotificationChannel channel, boolean updateMode) {
        String type = normalizeUpper(request.getType());
        if (!"WEBHOOK".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前仅支持 WEBHOOK 类型");
        }

        String name = normalize(request.getName());
        String webhookUrl = normalize(request.getWebhookUrl());
        validateWebhookUrl(webhookUrl);

        channel.setType(type);
        channel.setName(name);
        channel.setWebhookUrl(webhookUrl);
        if (!updateMode || request.getSecret() != null) {
            channel.setSecret(normalizeNullable(request.getSecret()));
        }
        channel.setEnabled(request.getEnabled() == null || request.getEnabled());
    }

    private void validateWebhookUrl(String webhookUrl) {
        try {
            URI uri = new URI(webhookUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "webhookUrl 必须是 http/https");
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "webhookUrl 缺少有效主机名");
            }
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "webhookUrl 格式不合法");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "字段不能为空");
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "字段不能为空");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }
}
