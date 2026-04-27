package com.aiops.monitor.controller;

import com.aiops.monitor.collector.SystemHardwareCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemInfoController {

    private final SystemHardwareCollector hardwareCollector;
    private final Environment environment;

    @Value("${monitor.mode:standalone}")
    private String monitorMode;

    @GetMapping("/hardware")
    public ResponseEntity<Map<String, Object>> getHardwareInfo() {
        String nodeName = environment.getProperty("spring.application.name", "Default-Node");
        String cpuModel = hardwareCollector.getCpuModel();
        if (cpuModel == null || cpuModel.isBlank()) {
            cpuModel = "Unknown CPU";
        }
        return ResponseEntity.ok(Map.of(
                "cpuModel", cpuModel,
                "totalMemoryBytes", hardwareCollector.getTotalMemoryBytes(),
                "hostname", nodeName,
                "mode", monitorMode
        ));
    }
}
