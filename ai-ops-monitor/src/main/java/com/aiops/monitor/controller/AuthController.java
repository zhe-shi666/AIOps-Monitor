package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AuthResponse;
import com.aiops.monitor.model.dto.ChangePasswordRequest;
import com.aiops.monitor.model.dto.LoginRequest;
import com.aiops.monitor.model.dto.PasswordResetRequest;
import com.aiops.monitor.model.dto.PasswordResetStartRequest;
import com.aiops.monitor.model.dto.RegisterRequest;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.UserRepository;
import com.aiops.monitor.security.JwtTokenProvider;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.PasswordPolicyService;
import com.aiops.monitor.service.PasswordResetMailService;
import com.aiops.monitor.service.PasswordResetService;
import com.aiops.monitor.service.UserNotificationProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final CurrentUserService currentUserService;
    private final PasswordPolicyService passwordPolicyService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetMailService passwordResetMailService;
    private final UserNotificationProfileService userNotificationProfileService;

    @Value("${security.registration.enabled:true}")
    private boolean registrationEnabled;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtTokenProvider.generateToken(userDetails);
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.isPasswordChangeRequired(),
                user.getNotificationEmail(),
                user.isNotificationEnabled()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前环境已关闭公开注册，请联系管理员创建账号");
        }
        passwordPolicyService.validate(request.getPassword());
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "邮箱已被注册"));
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setNotificationEmail(request.getEmail());
        user.setNotificationEnabled(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // 第一个注册的用户自动成为 ADMIN
        if (userRepository.count() == 0) {
            user.setRole(User.Role.ADMIN);
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "注册成功"));
    }

    @GetMapping("/me/notification-profile")
    public ResponseEntity<Map<String, Object>> getNotificationProfile(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(Map.of(
                "recipientEmail", user.getNotificationEmail(),
                "enabled", user.isNotificationEnabled()
        ));
    }

    @PutMapping("/me/notification-profile")
    public ResponseEntity<Map<String, Object>> updateNotificationProfile(@Valid @RequestBody com.aiops.monitor.model.dto.NotificationProfileRequest request,
                                                                         Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        User saved = userNotificationProfileService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(Map.of(
                "message", "接收邮箱已更新",
                "recipientEmail", saved.getNotificationEmail(),
                "enabled", saved.isNotificationEnabled()
        ));
    }

    @PostMapping("/password-reset/start")
    public ResponseEntity<Map<String, Object>> startPasswordReset(@Valid @RequestBody PasswordResetStartRequest request) {
        Optional<PasswordResetService.ResetTokenResult> result = passwordResetService.createResetToken(
                request.getUsername().trim(),
                request.getEmail().trim()
        );
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "如果用户名和邮箱匹配，系统将发送密码重置邮件");
        response.put("delivery", "EMAIL");
        if (result.isPresent()) {
            passwordResetMailService.sendResetLink(result.get().user(), result.get().rawToken());
            response.put("sent", true);
            response.put("expiresAt", result.get().expiresAt());
        } else {
            response.put("sent", false);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "密码已重置"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                              Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        passwordResetService.changePassword(user, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "密码已修改"));
    }
}
