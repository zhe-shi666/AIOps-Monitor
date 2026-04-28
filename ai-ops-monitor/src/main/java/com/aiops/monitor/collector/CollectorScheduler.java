package com.aiops.monitor.collector;

import com.aiops.monitor.model.dto.MetricDTO;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AiService;
import com.aiops.monitor.service.LocalCollectorOwnershipService;
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
    private LocalCollectorOwnershipService localCollectorOwnershipService;

    /*@Autowired
    private MetricsExporter metricsExporter;*/


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
        double diskUsage = hardwareCollector.getDiskUsage();
        double[] netUsage = hardwareCollector.getNetworkBytesPerSec();
        double netRxBytesPerSec = netUsage[0];
        double netTxBytesPerSec = netUsage[1];
        int processCount = hardwareCollector.getProcessCount();
        long timestamp = System.currentTimeMillis();

        // 0. 统一获取节点名称，避免多次重复调用
        String nodeName = environment.getProperty("spring.application.name", "Default-Node");
        LocalCollectorOwnershipService.LocalOwnership ownership = localCollectorOwnershipService.resolve(nodeName);
        String effectiveHostname = ownership == null ? nodeName : ownership.hostname();

        // 1. 存入数据库
        SystemMetricsHistory history = new SystemMetricsHistory();
        history.setCpuUsage(cpuUsage);
        history.setMemUsage(memUsage);
        history.setDiskUsage(diskUsage);
        history.setNetRxBytesPerSec(netRxBytesPerSec);
        history.setNetTxBytesPerSec(netTxBytesPerSec);
        history.setProcessCount(processCount);
        history.setTimestamp(java.time.LocalDateTime.now());
        history.setHostname(effectiveHostname);
        if (ownership != null) {
            history.setUserId(ownership.userId());
            history.setTargetId(ownership.targetId());
        }
        metricsRepository.save(history);

        // 2. 构建并发送给前端 CPU 指标
        MetricDTO cpuMetric = MetricDTO.builder()
                .name("CPU")
                .value(cpuUsage)
                .ip("127.0.0.1")
                .hostname(effectiveHostname)
                .timestamp(timestamp)
                .build();
        metricsPublisher.send("/topic/metrics", cpuMetric);

        // 3. 构建并发送给前端内存指标
        MetricDTO memMetric = MetricDTO.builder()
                .name("MEMORY")
                .value(memUsage)
                .ip("127.0.0.1")
                .hostname(effectiveHostname)
                .timestamp(timestamp)
                .build();
        metricsPublisher.send("/topic/metrics", memMetric);

        MetricDTO diskMetric = MetricDTO.builder()
                .name("DISK")
                .value(diskUsage)
                .ip("127.0.0.1")
                .hostname(effectiveHostname)
                .timestamp(timestamp)
                .build();
        metricsPublisher.send("/topic/metrics", diskMetric);

        MetricDTO netRxMetric = MetricDTO.builder()
                .name("NET_RX")
                .value(netRxBytesPerSec)
                .ip("127.0.0.1")
                .hostname(effectiveHostname)
                .timestamp(timestamp)
                .build();
        metricsPublisher.send("/topic/metrics", netRxMetric);

        MetricDTO netTxMetric = MetricDTO.builder()
                .name("NET_TX")
                .value(netTxBytesPerSec)
                .ip("127.0.0.1")
                .hostname(effectiveHostname)
                .timestamp(timestamp)
                .build();
        metricsPublisher.send("/topic/metrics", netTxMetric);

        MetricDTO processMetric = MetricDTO.builder()
                .name("PROCESS_COUNT")
                .value((double) processCount)
                .ip("127.0.0.1")
                .hostname(effectiveHostname)
                .timestamp(timestamp)
                .build();
        metricsPublisher.send("/topic/metrics", processMetric);

        log.debug("📡 [{}] 实时指标已推送并入库: CPU {}%, MEM {}%, DISK {}%, RX {}B/s, TX {}B/s, PROC {}",
                effectiveHostname,
                String.format("%.1f", cpuUsage),
                String.format("%.1f", memUsage),
                String.format("%.1f", diskUsage),
                String.format("%.0f", netRxBytesPerSec),
                String.format("%.0f", netTxBytesPerSec),
                processCount);

//        metricsExporter.updateMetrics(cpuUsage, memUsage);
    }

    // 任务 B：每 60 秒进行一次 AI 深度预测
    @Scheduled(fixedRate = 60000)
    public void aiPredictiveMaintenance() {
        // 1. 获取当前节点名称
        String currentHost = environment.getProperty("spring.application.name", "Default-Node");
        LocalCollectorOwnershipService.LocalOwnership ownership = localCollectorOwnershipService.resolve(currentHost);
        String effectiveHostname = ownership == null ? currentHost : ownership.hostname();

        // 2. 根据模式决定查询范围（策略分流）
        List<SystemMetricsHistory> history;
        String analysisScope;
        boolean isDistributed = "distributed".equalsIgnoreCase(monitorMode);

        if (isDistributed) {
            // 分布式模式：上帝视角，查全表
            history = metricsRepository.findTop20ByOrderByTimestampDesc();
            analysisScope = "全集群整体";
        } else {
            // 本地模式：保镖视角，只看自己
            history = metricsRepository.findTop20ByHostnameOrderByTimestampDesc(effectiveHostname);
            analysisScope = "本节点 (" + effectiveHostname + ")";
        }

        if (history.isEmpty()) return;

        // 3. 构建上下文并调用 AI
        String context = dataBuilder.buildMetricContext(history,isDistributed);
        String prompt = String.format("你是运维专家。请对 %s 的历史指标进行分析预测：%s", analysisScope, context);

        // 调用 aiService (注意：如果是分布式模式，这里建议配合我之前说的 Redis 锁)
        String prediction = aiService.analyzeData(prompt);

        log.info("🔮 [{}] AI 预测结论：{}", monitorMode.toUpperCase(), prediction);
        metricsPublisher.send("/topic/ai-reports", prediction);
    }
}
