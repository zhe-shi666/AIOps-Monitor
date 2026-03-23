package com.aiops.monitor.collector;

import com.aiops.monitor.model.dto.MetricDTO;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AiService;
import com.aiops.monitor.service.PromptDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CollectorScheduler {

    private final SystemHardwareCollector hardwareCollector;

    private final SystemMetricsRepository metricsRepository;

    @Autowired
    private AiService aiService;
    @Autowired
    private PromptDataBuilder dataBuilder;
    @Autowired
    private com.aiops.monitor.service.MetricsPublisher metricsPublisher;


    @Autowired
    private MetricsExporter metricsExporter;


    @Value("${monitor.mode:standalone}")
    private String monitorMode; // 注入模式：standalone 或 distributed

    @Autowired
    private Environment environment;

    public CollectorScheduler(SystemHardwareCollector hardwareCollector,
                              SystemMetricsRepository metricsRepository) {
        this.hardwareCollector = hardwareCollector;
        this.metricsRepository = metricsRepository;
    }

    // 任务 A：每 5 秒推送一次实时数据给 ECharts
    @Scheduled(fixedRate = 5000)
    public void collectAndPush() {
        double cpuUsage = hardwareCollector.getCpuUsage();
        double memUsage = hardwareCollector.getMemoryUsage();

        // 0. 统一获取节点名称，避免多次重复调用
        String nodeName = environment.getProperty("spring.application.name", "Default-Node");

        // 1. 存入数据库 (关键修复点：这里之前漏了 setHostname)
        SystemMetricsHistory history = new SystemMetricsHistory();
        history.setCpuUsage(cpuUsage);
        history.setMemUsage(memUsage);
        history.setTimestamp(java.time.LocalDateTime.now());
        history.setHostname(nodeName); // ✨ 必须设置这个，否则 SQL 过滤会失效
        metricsRepository.save(history);

        // 2. 构建并发送 CPU 指标 (代码已复用 nodeName)
        MetricDTO cpuMetric = MetricDTO.builder()
                .name("CPU")
                .value(cpuUsage)
                .ip("127.0.0.1")
                .hostname(nodeName)
                .timestamp(System.currentTimeMillis())
                .build();
        metricsPublisher.send("/topic/metrics", cpuMetric);

        // 3. 构建并发送内存指标
        MetricDTO memMetric = MetricDTO.builder()
                .name("MEMORY")
                .value(memUsage)
                .ip("127.0.0.1")
                .hostname(nodeName)
                .timestamp(System.currentTimeMillis())
                .build();
        metricsPublisher.send("/topic/metrics", memMetric);

        log.debug("📡 [{}] 实时指标已推送并入库: CPU {}%, MEM {}%",
                nodeName, String.format("%.1f", cpuUsage), String.format("%.1f", memUsage));

        metricsExporter.updateMetrics(cpuUsage, memUsage);
    }

    // 任务 B：每 60 秒进行一次 AI 深度预测
    @Scheduled(fixedRate = 60000)
    public void aiPredictiveMaintenance() {
        // 1. 获取当前节点名称
        String currentHost = environment.getProperty("spring.application.name", "Default-Node");

        // 2. 根据模式决定查询范围（策略分流）
        List<SystemMetricsHistory> history;
        String analysisScope;

        if ("distributed".equalsIgnoreCase(monitorMode)) {
            // 分布式模式：上帝视角，查全表
            history = metricsRepository.findTop20ByOrderByTimestampDesc();
            analysisScope = "全集群整体";
        } else {
            // 本地模式：保镖视角，只看自己
            history = metricsRepository.findTop20ByHostnameOrderByTimestampDesc(currentHost);
            analysisScope = "本节点 (" + currentHost + ")";
        }

        if (history.isEmpty()) return;

        // 3. 构建上下文并调用 AI
        String context = dataBuilder.buildMetricContext(history);
        String prompt = String.format("你是运维专家。请对 %s 的历史指标进行分析预测：%s", analysisScope, context);

        // 调用 aiService (注意：如果是分布式模式，这里建议配合我之前说的 Redis 锁)
        String prediction = aiService.analyzeData(prompt);

        log.info("🔮 [{}] AI 预测结论：{}", monitorMode.toUpperCase(), prediction);
        metricsPublisher.send("/topic/ai-reports", prediction);
    }
}