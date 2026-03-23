package com.aiops.monitor.collector;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AiService;
import com.aiops.monitor.service.PromptDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class AlarmCheckTask {

    @Autowired
    private SystemHardwareCollector hardwareCollector;

    @Autowired
    private IncidentLogRepository logRepository;

    @Autowired
    private SystemMetricsRepository metricsRepository;

    @Autowired
    private PromptDataBuilder dataBuilder;

    @Autowired
    private AiService aiService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 设定告警阈值
    private static final double CPU_THRESHOLD = 5.0;  // CPU 超过 70% 告警
    private static final double MEM_THRESHOLD = 80.0;  // 内存超过 90% 告警

    @Scheduled(fixedRate = 5000) // 每 5 秒执行一次检测
    public void checkSystemHealth() {
        double cpuUsage = hardwareCollector.getCpuUsage();
        double memUsage = hardwareCollector.getMemoryUsage();

        // 1. 检查 CPU
        if (cpuUsage > CPU_THRESHOLD) {
            recordIncident("CPU", cpuUsage, CPU_THRESHOLD);
        }

        // 2. 检查 内存
        if (memUsage > MEM_THRESHOLD) {
            recordIncident("MEMORY", memUsage, MEM_THRESHOLD);
        }

        log.info("【巡检中】CPU: {}%, 内存: {}%",
                String.format("%.2f", cpuUsage), String.format("%.2f", memUsage));
    }

    private void recordIncident(String name, double value, double threshold) {
        // 1. 存数据库（主线程执行，确保及时性）
        IncidentLog logEntry = new IncidentLog();
        // ... 设置属性
        logEntry.setMetricName(name);      // 这里一定要设置，name 是传进来的 "CPU" 或 "MEMORY"
        logEntry.setMetricValue(value);
        logEntry.setThreshold(threshold);
        logEntry.setCreatedAt(LocalDateTime.now());
        logEntry.setMessage(String.format("%s 使用率过高: %.2f%%", name, value));
        logRepository.save(logEntry);

        // 2. 扔给异步线程池处理 AI 逻辑
        String alertInfo = String.format("%s 使用率为 %.2f%%", name, value);
        List<SystemMetricsHistory> history = metricsRepository.findTop20ByOrderByTimestampDesc();
        String context = dataBuilder.buildMetricContext(history);

        // 调用异步方法，主线程会瞬间往下走
        aiService.getDiagnosticReportAsync(context, alertInfo)
                .thenAccept(report -> {
                    log.info("🤖 AI 诊断完成，正在推送至前端...");
                    // 将报告推送到 /topic/ai-reports 频道
                    messagingTemplate.convertAndSend("/topic/ai-reports", report);
                });
    }
}