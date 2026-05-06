package com.aiops.monitor.controller;

import com.aiops.monitor.model.entity.AnomalyResult;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.AnomalyResultRepository;
import com.aiops.monitor.service.AnomalyDetectionService;
import com.aiops.monitor.service.CurrentUserService;
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

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyResultRepository anomalyResultRepository;
    private final CurrentUserService currentUserService;
    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    public ResponseEntity<Page<AnomalyResult>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) Long targetId,
            Authentication authentication
    ) {
        User user = currentUserService.requireUser(authentication);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("detectedAt").descending());
        Page<AnomalyResult> result = anomalyResultRepository.search(
                normalizeUpper(status),
                normalizeUpper(metricKey),
                targetId,
                pageable
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication
    ) {
        User user = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(anomalyDetectionService.buildSummary(user.getId(), days));
    }

    private String normalizeUpper(String input) {
        if (input == null) return null;
        String value = input.trim();
        return value.isEmpty() ? null : value.toUpperCase(Locale.ROOT);
    }
}
