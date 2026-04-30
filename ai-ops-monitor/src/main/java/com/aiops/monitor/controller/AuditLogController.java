package com.aiops.monitor.controller;

import com.aiops.monitor.model.entity.AuditLog;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.AuditLogRepository;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.RoleGuardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final RoleGuardService roleGuardService;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) String action,
                                                @RequestParam(required = false) String resourceType,
                                                @RequestParam(required = false) String keyword,
                                                Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        PageRequest pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        Long userScope = roleGuardService.isAdmin(authentication) ? null : user.getId();
        return ResponseEntity.ok(auditLogRepository.search(
                userScope,
                blankToNull(action),
                blankToNull(resourceType),
                blankToNull(keyword),
                pageable
        ));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
