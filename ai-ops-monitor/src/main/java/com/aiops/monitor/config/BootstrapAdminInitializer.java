package com.aiops.monitor.config;

import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.UserRepository;
import com.aiops.monitor.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;

    @Value("${security.bootstrap-admin.enabled:false}")
    private boolean enabled;

    @Value("${security.bootstrap-admin.username:}")
    private String username;

    @Value("${security.bootstrap-admin.email:}")
    private String email;

    @Value("${security.bootstrap-admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (userRepository.count() > 0) {
            log.info("bootstrap admin skipped: users already exist");
            return;
        }
        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_* must be configured when bootstrap admin is enabled");
        }
        passwordPolicyService.validate(password);
        User admin = new User();
        admin.setUsername(username.trim());
        admin.setEmail(email.trim());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(User.Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("bootstrap admin created: {}", admin.getUsername());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
