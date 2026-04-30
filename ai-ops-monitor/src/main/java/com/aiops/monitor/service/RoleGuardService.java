package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleGuardService {
    public void requireOperator(User user) {
        if (user == null || user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.OPS) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色仅允许查看，不能执行写操作");
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(item -> "ROLE_ADMIN".equals(item.getAuthority()));
    }
}
