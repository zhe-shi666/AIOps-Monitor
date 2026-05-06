package com.aiops.monitor.collector;

import com.aiops.monitor.model.entity.IncidentLog;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AiService;
import com.aiops.monitor.service.InvestigationOrchestrator;
import com.aiops.monitor.service.LocalCollectorOwnershipService;
import com.aiops.monitor.service.NotificationDispatcherService;
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

    @Autowired
    private LocalCollectorOwnershipService localCollectorOwnershipService;

    @Autowired
    private InvestigationOrchestrator investigationOrchestrator;

    @Autowired
    private NotificationDispatcherService notificationDispatcherService;

    @Value("${monitor.investigation.auto-open-from-incident:false}")
    private boolean autoOpenInvestigationFromIncident;

    @Value("${monitor.mode:standalone}")
    private String monitorMode;

    @Autowired
    private Environment environment;

    @Value("${monitor.threshold.cpu:70}")
    private double cpuThreshold;

    @Value("${monitor.threshold.memory:90}")
    private double memoryThreshold;

    @Value("${monitor.threshold.disk:85}")
    private double diskThreshold;

    @Value("${monitor.threshold.process-count:400}")
    private double processCountThreshold;

    @Value("${monitor.threshold.net-rx:10485760}")
    private double netRxThreshold;

    @Value("${monitor.threshold.net-tx:10485760}")
    private double netTxThreshold;

    @Value("${monitor.escalation.p2-intervals:5,15,30}")
    private String p2Intervals;

    @Scheduled(fixedRate = 5000) // 每 5 秒执行一次检测
    public void checkSystemHealth() {
        double cpuUsage = hardwareCollector.getCpuUsage();
        double memUsage = hardwareCollector.getMemoryUsage();
        double diskUsage = hardwareCollector.getDiskUsage();
        double[] netUsage = hardwareCollector.getNetworkBytesPerSec();
        double netRxBytesPerSec = netUsage[0];
        double netTxBytesPerSec = netUsage[1];
        int processCount = hardwareCollector.getProcessCount();

        // 1. 检查 CPU
        if (cpuUsage > cpuThreshold) {
            recordIncident("CPU", cpuUsage, cpuThreshold);
        }

        // 2. 检查 内存
        if (memUsage > memoryThreshold) {
            recordIncident("MEMORY", memUsage, memoryThreshold);
        }

        if (diskUsage > diskThreshold) {
            recordIncident("DISK", diskUsage, diskThreshold);
        }

        if (processCount > processCountThreshold) {
            recordIncident("PROCESS_COUNT", processCount, processCountThreshold);
        }

        if (netRxBytesPerSec > netRxThreshold) {
            recordIncident("NET_RX", netRxBytesPerSec, netRxThreshold);
        }

        if (netTxBytesPerSec > netTxThreshold) {
            recordIncident("NET_TX", netTxBytesPerSec, netTxThreshold);
        }

        log.info("【巡检中】CPU: {}%, MEM: {}%, DISK: {}%, RX: {}B/s, TX: {}B/s, PROC: {}",
                String.format("%.2f", cpuUsage),
                String.format("%.2f", memUsage),
                String.format("%.2f", diskUsage),
                String.format("%.0f", netRxBytesPerSec),
                String.format("%.0f", netTxBytesPerSec),
                processCount);
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
        LocalCollectorOwnershipService.LocalOwnership ownership = localCollectorOwnershipService.resolve(currentHost);
        String effectiveHostname = ownership == null ? currentHost : ownership.hostname();

        logEntry.setHostname(effectiveHostname);
        if (ownership != null) {
            logEntry.setUserId(ownership.userId());
            logEntry.setTargetId(ownership.targetId());
        }
        logEntry.setMetricName(name);
        logEntry.setMetricValue(value);
        logEntry.setThreshold(threshold);
        LocalDateTime now = LocalDateTime.now();
        logEntry.setCreatedAt(now);
        String unit = resolveUnit(name);
        String display = "%".equals(unit) ? String.format("%.2f%%", value) : String.format("%.2f%s", value, unit);
        String thresholdDisplay = "%".equals(unit) ? String.format("%.2f%%", threshold) : String.format("%.2f%s", threshold, unit);
        logEntry.setMessage(String.format("%s 指标超过阈值: %s > %s", name, display, thresholdDisplay));
        logEntry.setStatus("OPEN");
        logEntry.setSeverity(calculateSeverity(value, threshold));
        logEntry.setEscalationLevel(0);
        logEntry.setLastNotifiedAt(now);
        Integer firstIntervalMinutes = parseFirstIntervalMinutes();
        logEntry.setNextNotifyAt(firstIntervalMinutes == null ? null : now.plusMinutes(firstIntervalMinutes));
        logEntry.setOccurrenceCount(1);
        logEntry.setSuppressedCount(0);
        logEntry.setFirstSeenAt(now);
        logEntry.setLastSeenAt(now);
        logEntry.setSourceType("METRIC");
        logEntry.setSourceRef(ownership == null || ownership.targetId() == null ? null : "target:" + ownership.targetId());
        IncidentLog saved = logRepository.save(logEntry);
        notificationDispatcherService.dispatchIncidentOpened(saved);
        if (autoOpenInvestigationFromIncident) {
            try {
                investigationOrchestrator.openFromIncident(saved);
            } catch (Exception ex) {
                log.warn("本地告警创建调查失败: incidentId={}, reason={}", saved.getId(), ex.getMessage());
            }
        }


        // 2. 根据模式决定扔给异步线程池处理 AI 逻辑
        List<SystemMetricsHistory> history;
        String analysisScope;
        boolean isDistributed = "distributed".equalsIgnoreCase(monitorMode);
        String alertInfo;
        if (isDistributed) {
            // 分布式模式：上帝视角，查全表
            alertInfo= String.format("%s中%s 使用率为 %.2f%%",effectiveHostname, name, value);
            history = metricsRepository.findTop20ByOrderByTimestampDesc();
            analysisScope = "全集群整体";
        } else {
            // 本地模式：保镖视角，只看自己
            alertInfo = String.format("%s 使用率为 %.2f%%", name, value);
            history = metricsRepository.findTop20ByHostnameOrderByTimestampDesc(effectiveHostname);
            analysisScope = "本节点 (" + effectiveHostname + ")";
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

    private String resolveUnit(String metricName) {
        if ("CPU".equalsIgnoreCase(metricName)
                || "MEMORY".equalsIgnoreCase(metricName)
                || "DISK".equalsIgnoreCase(metricName)) {
            return "%";
        }
        if ("PROCESS_COUNT".equalsIgnoreCase(metricName)) {
            return "";
        }
        return "B/s";
    }

    private String calculateSeverity(double value, double threshold) {
        if (threshold <= 0) {
            return "P2";
        }
        double ratio = value / threshold;
        if (ratio >= 1.5d) {
            return "P1";
        }
        if (ratio >= 1.2d) {
            return "P2";
        }
        return "P3";
    }

    private Integer parseFirstIntervalMinutes() {
        if (p2Intervals == null || p2Intervals.isBlank()) {
            return 5;
        }
        String first = p2Intervals.split(",")[0].trim();
        try {
            return Integer.parseInt(first);
        } catch (NumberFormatException ex) {
            return 5;
        }
    }
}
