package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.PlatformMailSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class PlatformMailSenderService {

    private final PlatformMailSettingsService platformMailSettingsService;

    public PlatformMailSettings requireEnabledSettings() {
        PlatformMailSettings settings = platformMailSettingsService.getCurrent();
        if (settings == null || !settings.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "管理员尚未启用邮件发信通道");
        }
        if (isBlank(settings.getSmtpHost()) || isBlank(settings.getSmtpUsername()) || isBlank(settings.getSmtpPassword())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "管理员尚未完成 SMTP 配置");
        }
        return settings;
    }

    public void sendSimpleMail(String to, String subject, String text) {
        PlatformMailSettings settings = requireEnabledSettings();
        if (isBlank(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收件邮箱不能为空");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.getFromAddress().trim());
        message.setTo(to.trim());
        message.setSubject(subject);
        message.setText(text);

        buildSender(settings).send(message);
    }

    private JavaMailSenderImpl buildSender(PlatformMailSettings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getSmtpHost().trim());
        sender.setPort(settings.getSmtpPort() == null ? 587 : settings.getSmtpPort());
        sender.setUsername(settings.getSmtpUsername().trim());
        sender.setPassword(settings.getSmtpPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(settings.isSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.isSmtpStarttls()));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
