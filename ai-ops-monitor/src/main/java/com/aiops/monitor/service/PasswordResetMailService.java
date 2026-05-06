package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PasswordResetMailService {
    private final PlatformMailSenderService platformMailSenderService;

    @Value("${security.password-reset.mail-enabled:false}")
    private boolean mailEnabled;

    @Value("${security.password-reset.frontend-reset-url:http://localhost:5173/reset-password}")
    private String frontendResetUrl;

    @Value("${security.password-reset.token-ttl-minutes:30}")
    private int tokenTtlMinutes;

    public boolean isMailEnabled() {
        return mailEnabled;
    }

    public void sendResetLink(User user, String rawToken) {
        if (!mailEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "密码重置邮件未启用，请联系管理员配置 SMTP");
        }

        String resetLink = frontendResetUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        platformMailSenderService.sendSimpleMail(
                user.getEmail(),
                "AIOps Monitor 密码重置",
                "您好，" + user.getUsername() + "：\n\n"
                        + "我们收到了您的密码重置请求。请在 " + tokenTtlMinutes + " 分钟内打开以下链接设置新密码：\n\n"
                        + resetLink + "\n\n"
                        + "如果不是您本人操作，请忽略此邮件。\n\n"
                        + "AIOps Monitor"
        );
    }
}
