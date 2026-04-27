package com.aiops.monitor.controller;

import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final IncidentLogRepository incidentLogRepository;
    private final MonitorTargetRepository monitorTargetRepository;

    @GetMapping("/users")
    public ResponseEntity<Page<User>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result = (username != null && !username.isBlank())
                ? userRepository.findByUsernameContainingIgnoreCase(username, pageable)
                : userRepository.findAll(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "user", user,
                "targets", monitorTargetRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
        ));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElseThrow();
        user.setRole(User.Role.valueOf(body.get("role")));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "角色已更新"));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(body.get("enabled"));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "状态已更新"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = userRepository.count();
        long totalTargets = monitorTargetRepository.count();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayIncidents = incidentLogRepository.countByCreatedAtAfter(todayStart);
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalTargets", totalTargets,
                "todayIncidents", todayIncidents
        ));
    }
}
