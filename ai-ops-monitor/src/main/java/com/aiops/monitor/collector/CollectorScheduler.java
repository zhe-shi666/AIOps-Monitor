package com.aiops.monitor.collector;

import com.aiops.monitor.model.dto.MetricDTO;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AiService;
import com.aiops.monitor.service.PromptDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CollectorScheduler {

    private final SystemHardwareCollector hardwareCollector;

    @Autowired
    private SystemMetricsRepository metricsRepository;
    @Autowired
    private AiService aiService;
    @Autowired
    private PromptDataBuilder dataBuilder;
    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public CollectorScheduler(SystemHardwareCollector hardwareCollector) {
        this.hardwareCollector = hardwareCollector;
    }

    // 任务 A：每 5 秒推送一次实时数据给 ECharts
    @Scheduled(fixedRate = 5000)
    public void collectAndPush() {
        // 1. 获取硬件实时数据
        double cpuUsage = hardwareCollector.getCpuUsage();
        double memUsage = hardwareCollector.getMemoryUsage();

        // 2. 【缺失的关键步骤】保存到数据库历史表
        SystemMetricsHistory history = new SystemMetricsHistory();
        history.setCpuUsage(cpuUsage);
        history.setMemUsage(memUsage);
        history.setTimestamp(java.time.LocalDateTime.now());

        // 执行保存
        metricsRepository.save(history);
        log.debug("💾 已存入历史数据库: CPU={}%, MEM={}%", cpuUsage, memUsage);

        // 3. 原有的推送逻辑 (发送给前端 ECharts)
        messagingTemplate.convertAndSend("/topic/metrics", new MetricDTO("CPU", cpuUsage));
        messagingTemplate.convertAndSend("/topic/metrics", new MetricDTO("MEMORY", memUsage));

        log.debug("📡 实时指标已推送: CPU {}%, MEM {}%", String.format("%.1f", cpuUsage), String.format("%.1f", memUsage));
    }

    // 任务 B：每 60 秒进行一次 AI 深度预测
    @Scheduled(fixedRate = 60000)
    public void aiPredictiveMaintenance() {
        List<SystemMetricsHistory> history = metricsRepository.findTop20ByOrderByTimestampDesc();
        if (history.isEmpty()) return;

        String context = dataBuilder.buildMetricContext(history);
        String prediction = aiService.analyzeData("根据以下趋势，预测未来10分钟系统风险？格式：【风险判断】有/无风险。理由：... " + context);

        log.info("🔮 AI 预测：{}", prediction);
        // 推送 AI 预测结论到报告频道
        messagingTemplate.convertAndSend("/topic/ai-reports", prediction);
    }
}