package com.aiops.monitor.collector;

import com.aiops.monitor.controller.MonitorWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MetricsPushScheduler {

    @Autowired
    private SystemHardwareCollector hardwareCollector;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 每 1000 毫秒 (1秒) 执行一次
    @Scheduled(fixedRate = 1000)
    public void pushRealtimeMetrics() {
        try {
            // 组装实时数据 DTO
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("type", "REALTIME_METRICS");
            metrics.put("cpu", hardwareCollector.getCpuUsage());
            metrics.put("mem", hardwareCollector.getMemoryUsage());
            metrics.put("timestamp", System.currentTimeMillis() / 1000);

            String json = objectMapper.writeValueAsString(metrics);

            // 调用 Handler 的广播方法
            MonitorWebSocketHandler.broadcast(json);

        } catch (Exception e) {
            log.error("推送实时数据失败: {}", e.getMessage());
        }
    }
}