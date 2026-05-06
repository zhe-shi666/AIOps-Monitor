package com.aiops.monitor.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyServiceTest {

    private PasswordPolicyService strictPolicy() {
        PasswordPolicyService service = new PasswordPolicyService();
        ReflectionTestUtils.setField(service, "minLength", 10);
        ReflectionTestUtils.setField(service, "requireUppercase", true);
        ReflectionTestUtils.setField(service, "requireLowercase", true);
        ReflectionTestUtils.setField(service, "requireDigit", true);
        ReflectionTestUtils.setField(service, "requireSpecial", true);
        return service;
    }

    @Test
    void shouldAcceptEnterpriseStrengthPassword() {
        assertDoesNotThrow(() -> strictPolicy().validate("AIOps@2026"));
    }

    @Test
    void shouldRejectWeakPasswords() {
        PasswordPolicyService policy = strictPolicy();

        assertThrows(ResponseStatusException.class, () -> policy.validate("short1A!"));
        assertThrows(ResponseStatusException.class, () -> policy.validate("aiopsmonitor1!"));
        assertThrows(ResponseStatusException.class, () -> policy.validate("AIOPSMONITOR1!"));
        assertThrows(ResponseStatusException.class, () -> policy.validate("AIOpsMonitor!"));
        assertThrows(ResponseStatusException.class, () -> policy.validate("AIOpsMonitor1"));
    }
}
