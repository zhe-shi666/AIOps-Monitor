package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleGuardServiceTest {
    private final RoleGuardService roleGuardService = new RoleGuardService();

    @Test
    void shouldAllowAdminAndOpsToWrite() {
        assertDoesNotThrow(() -> roleGuardService.requireOperator(user(User.Role.ADMIN)));
        assertDoesNotThrow(() -> roleGuardService.requireOperator(user(User.Role.OPS)));
    }

    @Test
    void shouldRejectAuditorAndUserWriteActions() {
        assertThrows(ResponseStatusException.class, () -> roleGuardService.requireOperator(user(User.Role.AUDITOR)));
        assertThrows(ResponseStatusException.class, () -> roleGuardService.requireOperator(user(User.Role.USER)));
    }

    private User user(User.Role role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
