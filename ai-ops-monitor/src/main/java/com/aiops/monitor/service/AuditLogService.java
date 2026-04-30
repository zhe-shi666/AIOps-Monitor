package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AuditLog;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public void record(Authentication authentication,
                       HttpServletRequest request,
                       String action,
                       String resourceType,
                       Long resourceId,
                       Map<String, Object> detail) {
        try {
            User user = currentUserService.requireUser(authentication);
            AuditLog logEntry = new AuditLog();
            logEntry.setUserId(user.getId());
            logEntry.setActor(user.getUsername());
            logEntry.setAction(action);
            logEntry.setResourceType(resourceType);
            logEntry.setResourceId(resourceId);
            logEntry.setIpAddress(resolveIp(request));
            logEntry.setDetailJson(detail == null ? null : objectMapper.writeValueAsString(detail));
            logEntry.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("audit log write failed, action={}, resourceType={}, reason={}", action, resourceType, ex.getMessage());
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
