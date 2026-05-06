package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemporaryPasswordServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TemporaryPasswordService service = new TemporaryPasswordService(userRepository, passwordEncoder);

    @Test
    void shouldIssueTemporaryPasswordAndRequirePasswordChange() {
        User user = new User();
        user.setId(9L);
        user.setUsername("demo");
        user.setPassword("old");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String temporaryPassword = service.issueTemporaryPassword(user);

        assertNotNull(temporaryPassword);
        assertTrue(temporaryPassword.length() >= 14);
        assertNotEquals("old", temporaryPassword);
        assertTrue(passwordEncoder.matches(temporaryPassword, user.getPassword()));
        assertTrue(user.isPasswordChangeRequired());
        assertNotNull(user.getTemporaryPasswordIssuedAt());
        verify(userRepository).save(user);
    }
}
