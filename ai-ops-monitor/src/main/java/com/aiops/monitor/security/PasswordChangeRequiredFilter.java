package com.aiops.monitor.security;

import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    private static final String CHANGE_PASSWORD_PATH = "/api/auth/change-password";

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") || path.startsWith("/actuator/") || path.startsWith("/ws-monitor/")) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.isPasswordChangeRequired()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "message", "当前账号使用临时密码登录，必须先修改密码",
                    "code", "PASSWORD_CHANGE_REQUIRED"
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
