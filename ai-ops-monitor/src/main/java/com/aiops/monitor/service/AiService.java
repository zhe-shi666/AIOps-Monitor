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

    // required = false 确保单机模式没有 Redis 也能启动
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // 本地模式锁的状态变量
    private long lastLocalCallTime = 0;
    private static final long COOLDOWN_MS = 30000; // 30秒内不重复触发 AI
    private static final String REDIS_COOLDOWN_KEY = "aiops:diagnostic:cooldown";

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        // 配置系统级 Prompt，确立 AI 的角色与输出规范
        this.chatClient = builder
                .defaultSystem("""
                    你是一个资深的 Java 架构师与 AIOps 智能运维专家。
                    你的职责是根据历史指标分析系统风险。
                    重点关注：【内存泄漏】、【CPU 死循环】、【集群负载不均】。
                    回答要求：使用简洁的 Markdown 格式，必须包含具体的 Linux 排查命令。
                    """)
                .build();
    }

    /**
     * 原子化尝试抢占诊断锁 (自适应模式)
     * 核心亮点：解决了分布式竞争，且实现了单机/分布式降级。
     */
    private boolean tryAcquireDiagnosticLock() {
        // 1. 分布式锁逻辑：如果能连上 Redis，优先使用分布式锁
        if (redisTemplate != null) {
            try {
                // SETNX 原子指令：如果 key 不存在则设置并返回 true，反之 false
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(REDIS_COOLDOWN_KEY, "LOCKED", Duration.ofMillis(COOLDOWN_MS));
                return Boolean.TRUE.equals(success);
            } catch (Exception e) {
                log.warn("📡 Redis 访问失败，将自动降级为本地内存锁控制。原因: {}", e.getMessage());
            }
        }

        // 2. 本地锁逻辑：备选方案，利用 synchronized 保证单机并发安全
        synchronized (this) {
            long now = System.currentTimeMillis();
            if (now - lastLocalCallTime < COOLDOWN_MS) {
                return false;
            }
            lastLocalCallTime = now;
            return true;
        }
    }

    /**
     * 异步深度诊断接口
     * 被 @Async 标注，调用时主线程会立即返回，AI 逻辑在独立线程池运行
     */
    @Async("taskExecutor")
    public CompletableFuture<String> getDiagnosticReportAsync(String metricContext, String alertInfo) {

        // --- 核心防护：并发与冷却检测 ---
        if (!tryAcquireDiagnosticLock()) {
            log.info("🧊 [冷却中] 已拦截高频 AI 诊断请求，防止资源浪费。模式: {}", monitorMode.toUpperCase());
            return CompletableFuture.completedFuture("AI 正在冷静期，请稍后再试。");
        }

        log.info("🤖 [AI 启动] 正在为节点进行故障根因分析 (RCA)...");
        long startTime = System.currentTimeMillis();

        try {
            // 构造用户端提示词
            String userPrompt = String.format("""
                ### 🚨 实时告警快照
                %s

                ### 📊 节点历史趋势 (Context)
                %s
                
                请按以下维度输出诊断报告：
                1. **风险评级**：(正常/警告/紧急)
                2. **根因推测**：分析指标抖动的可能诱因。
                3. **排查指令**：给出 3 条最直接的 Linux 或 JVM 命令。
                """, alertInfo, metricContext);

            // 发起 AI 调用
            String report = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            log.info("✅ [AI 完成] 诊断耗时: {} ms", System.currentTimeMillis() - startTime);
            return CompletableFuture.completedFuture(report);

        } catch (Exception e) {
            log.error("❌ [AI 异常] 诊断过程中发生错误: {}", e.getMessage());
            // 如果 AI 调用失败，清除 Redis 锁，允许下次快速重试
            if (redisTemplate != null) redisTemplate.delete(REDIS_COOLDOWN_KEY);
            return CompletableFuture.completedFuture("诊断暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 通用的简单数据分析接口（同步调用）
     */
    public String analyzeData(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}