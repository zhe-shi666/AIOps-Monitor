package com.aiops.monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiService {

    @Value("${monitor.mode:standalone}")
    private String monitorMode;

    // 使用 required = false 确保在本地模式没有 Redis 时不报错
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private long lastLocalCallTime = 0;
    private static final long COOLDOWN_MS = 30000; // 30秒冷却
    private static final String REDIS_COOLDOWN_KEY = "aiops:diagnostic:cooldown";

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                    你是一个资深的 Java 架构师与 AIOps 智能运维专家。
                    你的职责是分析分布式集群的监控指标，识别潜在系统风险。
                    请优先分析是否存在【内存泄漏】、【CPU 死循环】或【集群负载不均】。
                    回答要求：使用简洁的 Markdown 格式，并提供具体的 Linux 排查命令。
                    """)
                .build();
    }

    /**
     * 判断是否处于冷却期（支持本地/分布式自适应）
     */
    public boolean isCoolingDown() {
        if ("distributed".equalsIgnoreCase(monitorMode) && redisTemplate != null) {
            // 分布式模式：利用 Redis 的 key 是否存在来判断
            return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_COOLDOWN_KEY));
        } else {
            // 本地模式：利用内存时间戳判断
            return (System.currentTimeMillis() - lastLocalCallTime) < COOLDOWN_MS;
        }
    }

    /**
     * 锁定冷却时间
     */
    private void lockCooldown() {
        if ("distributed".equalsIgnoreCase(monitorMode) && redisTemplate != null) {
            // 在 Redis 中存入一个带过期时间的 key
            redisTemplate.opsForValue().set(REDIS_COOLDOWN_KEY, "locked", Duration.ofMillis(COOLDOWN_MS));
        } else {
            this.lastLocalCallTime = System.currentTimeMillis();
        }
    }

    /**
     * 同步分析接口（用于简单预测）
     */
    public String analyzeData(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 核心诊断接口：获取深度 Markdown 报告
     */
    public String getDiagnosticReport(String metricContext, String currentAlert) {
        String userPrompt = String.format("""
            ### 🚨 当前告警详情
            %s

            ### 📊 历史指标背景
            %s
            
            请根据以上信息进行深度诊断：
            1. **趋势分析**：判断是偶发抖动还是持续恶化。
            2. **根因推测**：分析可能的系统瓶颈。
            3. **行动建议**：给出排查指令。
            """, currentAlert, metricContext);

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 异步诊断接口：支持虚拟线程执行，带冷却保护
     */
    @Async("taskExecutor") // 使用你定义的异步线程池
    public CompletableFuture<String> getDiagnosticReportAsync(String context, String alertInfo) {
        // 1. 检查冷却状态
        if (isCoolingDown()) {
            log.info("🧊 AI 诊断正处于冷却保护期 (模式: {})，跳过本次请求。", monitorMode);
            return CompletableFuture.completedFuture("AI is cooling down...");
        }

        // 2. 立即锁定，防止并发进入
        lockCooldown();

        log.info("🤖 启动 AI 深度诊断分析...");
        long start = System.currentTimeMillis();

        try {
            String report = getDiagnosticReport(context, alertInfo);
            log.info("✅ AI 诊断完成，耗时: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(report);
        } catch (Exception e) {
            log.error("❌ AI 诊断发生异常: {}", e.getMessage());
            return CompletableFuture.completedFuture("AI 诊断失败: " + e.getMessage());
        }
    }
}