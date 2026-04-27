package com.aiops.monitor.collector;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AiService;
import com.aiops.monitor.service.PromptDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @Value("${monitor.mode:standalone}")
    private String monitorMode;

    @Autowired
    private Environment environment;

    @Value("${monitor.threshold.cpu:70}")
    private double cpuThreshold;

    @Value("${monitor.threshold.memory:90}")
    private double memoryThreshold;

    @Scheduled(fixedRate = 5000) // 每 5 秒执行一次检测
    public void checkSystemHealth() {
        double cpuUsage = hardwareCollector.getCpuUsage();
        double memUsage = hardwareCollector.getMemoryUsage();

        // 1. 检查 CPU
        if (cpuUsage > cpuThreshold) {
            recordIncident("CPU", cpuUsage, cpuThreshold);
        }

        // 2. 检查 内存
        if (memUsage > memoryThreshold) {
            recordIncident("MEMORY", memUsage, memoryThreshold);
        }

        log.info("【巡检中】CPU: {}%, 内存: {}%",
                String.format("%.2f", cpuUsage), String.format("%.2f", memUsage));
    }


    // 记录每个指标上次告警的时间，防止告警风暴
    private Map<String, LocalDateTime> lastAlarmTime = new ConcurrentHashMap<>();

    private void recordIncident(String name, double value, double threshold) {

        // 检查是否在静默期（例如 3 分钟内不再重复告警）
        LocalDateTime last = lastAlarmTime.get(name);
        if (last != null && last.plusMinutes(3).isAfter(LocalDateTime.now())) {
            return;
        }
        lastAlarmTime.put(name, LocalDateTime.now());

        // 0. 获取当前节点名称
        String currentHost = environment.getProperty("spring.application.name", "Default-Node");

        // 1. 存数据库（主线程执行，确保及时性）
        IncidentLog logEntry = new IncidentLog();
        // ... 设置属性
        logEntry.setHostname(currentHost);
        logEntry.setMetricName(name);
        logEntry.setMetricValue(value);
        logEntry.setThreshold(threshold);
        logEntry.setCreatedAt(LocalDateTime.now());
        logEntry.setMessage(String.format("%s 使用率过高: %.2f%%", name, value));
        logEntry.setStatus("OPEN");
        logRepository.save(logEntry);


        // 2. 根据模式决定扔给异步线程池处理 AI 逻辑
        List<SystemMetricsHistory> history;
        String analysisScope;
        boolean isDistributed = "distributed".equalsIgnoreCase(monitorMode);
        String alertInfo;
        if (isDistributed) {
            // 分布式模式：上帝视角，查全表
            alertInfo= String.format("%s中%s 使用率为 %.2f%%",currentHost, name, value);
            history = metricsRepository.findTop20ByOrderByTimestampDesc();
            analysisScope = "全集群整体";
        } else {
            // 本地模式：保镖视角，只看自己
            alertInfo = String.format("%s 使用率为 %.2f%%", name, value);
            history = metricsRepository.findTop20ByHostnameOrderByTimestampDesc(currentHost);
            analysisScope = "本节点 (" + currentHost + ")";
        }

        if (history.isEmpty()) return;

        String context = dataBuilder.buildMetricContext(history,isDistributed);

        // 调用异步方法，主线程会瞬间往下走
        aiService.getDiagnosticReportAsync(context, alertInfo)
                .thenAccept(report -> {
                    log.info("🤖 AI 诊断完成，正在推送至前端...");
                    // 将报告推送到 /topic/ai-reports 频道
                    messagingTemplate.convertAndSend("/topic/ai-reports", report);
                });
    }
}
