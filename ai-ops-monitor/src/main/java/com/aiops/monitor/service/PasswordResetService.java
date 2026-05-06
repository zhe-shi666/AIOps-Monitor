package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.PasswordResetToken;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.PasswordResetTokenRepository;
import com.aiops.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.password-reset.token-ttl-minutes:30}")
    private int tokenTtlMinutes;

    @Transactional
    public Optional<ResetTokenResult> createResetToken(String username, String email) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty() || user.get().getEmail() == null || !user.get().getEmail().equalsIgnoreCase(email)) {
            return Optional.empty();
        }

        String rawToken = randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.get().getId());
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        passwordResetTokenRepository.save(token);
        log.info("password reset token created for user={}, expiresAt={}", user.get().getUsername(), token.getExpiresAt());
        return Optional.of(new ResetTokenResult(user.get(), rawToken, token.getExpiresAt()));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        passwordPolicyService.validate(newPassword);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(sha256(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "重置链接无效或已使用"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "重置链接已过期");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordIssuedAt(null);
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void changePassword(User user, String newPassword) {
        passwordPolicyService.validate(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordIssuedAt(null);
        userRepository.save(user);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record ResetTokenResult(User user, String rawToken, LocalDateTime expiresAt) {}
}
