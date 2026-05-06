package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NotificationEmailService {
    private final PlatformMailSenderService platformMailSenderService;

    public boolean isEnabled() {
        try {
            return platformMailSenderService.requireEnabledSettings() != null;
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    public void sendIncidentMail(NotificationChannel channel, String eventType, IncidentLog incident) {
        if (isBlank(channel.getEmailTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱通知通道缺少收件人");
        }
        platformMailSenderService.sendSimpleMail(channel.getEmailTo(), buildSubject(eventType, incident), buildBody(eventType, incident));
    }

    public void sendIncidentMailToRecipient(String recipientEmail, String eventType, IncidentLog incident) {
        if (isBlank(recipientEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收件邮箱不能为空");
        }
        platformMailSenderService.sendSimpleMail(recipientEmail, buildSubject(eventType, incident), buildBody(eventType, incident));
    }

    public void sendPlatformTestMail(String recipientEmail) {
        if (isBlank(recipientEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "测试收件邮箱不能为空");
        }
        platformMailSenderService.sendSimpleMail(
                recipientEmail.trim(),
                "[AIOps][SMTP_TEST] 平台邮件通道测试",
                "这是一封来自 AIOps Monitor 的平台级邮件测试。\n\n如果你收到这封邮件，说明管理员配置的发信通道已生效。"
        );
    }

    private String buildSubject(String eventType, IncidentLog incident) {
        return "[AIOps][" + nullSafe(incident.getSeverity(), "P2") + "][" + eventType + "] "
                + nullSafe(incident.getHostname(), "unknown-host") + " "
                + nullSafe(incident.getMetricName(), "unknown-metric");
    }

    private String buildBody(String eventType, IncidentLog incident) {
        return "AIOps 告警通知\n\n"
                + "事件类型: " + eventType + "\n"
                + "告警ID: " + incident.getId() + "\n"
                + "状态: " + nullSafe(incident.getStatus(), "-") + "\n"
                + "级别: " + nullSafe(incident.getSeverity(), "-") + "\n"
                + "主机: " + nullSafe(incident.getHostname(), "-") + "\n"
                + "指标: " + nullSafe(incident.getMetricName(), "-") + "\n"
                + "当前值: " + nullSafe(incident.getMetricValue(), "-") + "\n"
                + "阈值: " + nullSafe(incident.getThreshold(), "-") + "\n"
                + "内容: " + nullSafe(incident.getMessage(), "-") + "\n";
    }

    private String nullSafe(Object value, Object fallback) {
        return String.valueOf(value == null ? fallback : value);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
