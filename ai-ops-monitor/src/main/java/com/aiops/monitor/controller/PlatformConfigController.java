package com.aiops.monitor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/platform")
public class PlatformConfigController {
    @Value("${monitor.agent.latest-version:agent-lite-1.2.0-cross}")
    private String latestAgentVersion;

    @GetMapping("/agent-release")
    public ResponseEntity<Map<String, Object>> agentRelease() {
        return ResponseEntity.ok(Map.of(
                "latestVersion", latestAgentVersion,
                "packageName", "aiops-agent-lite.tar.gz",
                "supports", new String[]{"linux", "macos", "windows"}
        ));
    }
}
