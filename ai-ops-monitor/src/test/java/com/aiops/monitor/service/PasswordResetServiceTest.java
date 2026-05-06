package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.PasswordResetToken;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.PasswordResetTokenRepository;
import com.aiops.monitor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
    private final PasswordResetService service = new PasswordResetService(
            userRepository,
            tokenRepository,
            passwordEncoder,
            passwordPolicyService
    );

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tokenTtlMinutes", 30);
    }

    @Test
    void shouldCreateResetTokenWhenUsernameAndEmailMatch() {
        User user = new User();
        user.setId(7L);
        user.setUsername("ops");
        user.setEmail("ops@example.com");
        when(userRepository.findByUsername("ops")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<PasswordResetService.ResetTokenResult> result = service.createResetToken("ops", "OPS@example.com");

        assertTrue(result.isPresent());
        assertEquals(user, result.get().user());
        assertEquals(64, result.get().rawToken().length());
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertEquals(7L, tokenCaptor.getValue().getUserId());
        assertNotEquals(result.get().rawToken(), tokenCaptor.getValue().getTokenHash());
        assertTrue(tokenCaptor.getValue().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldNotCreateResetTokenWhenEmailDoesNotMatch() {
        User user = new User();
        user.setId(7L);
        user.setUsername("ops");
        user.setEmail("ops@example.com");
        when(userRepository.findByUsername("ops")).thenReturn(Optional.of(user));

        Optional<PasswordResetService.ResetTokenResult> result = service.createResetToken("ops", "other@example.com");

        assertTrue(result.isEmpty());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void shouldResetPasswordAndMarkTokenUsed() {
        String rawToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(11L);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        User user = new User();
        user.setId(11L);
        user.setPassword("old");

        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));

        service.resetPassword(rawToken, "AIOps@2026");

        verify(passwordPolicyService).validate("AIOps@2026");
        assertTrue(passwordEncoder.matches("AIOps@2026", user.getPassword()));
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void shouldRejectExpiredResetToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(11L);
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));

        assertThrows(ResponseStatusException.class, () -> service.resetPassword("expired", "AIOps@2026"));
    }
}
