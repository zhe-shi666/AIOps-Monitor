package com.aiops.monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiService {

    private long lastCallTime = 0;
    private static final long COOLDOWN_MS = 30000; // 30秒内只允许一次 AI 深度诊断

    private final ChatClient chatClient;

    // Spring AI 推荐使用 ChatClient.Builder 来构建客户端
    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个 AIOps 智能运维助手。你的职责是分析 Prometheus 监控指标，识别系统风险。请用简洁、专业的语言回答，并在可能的情况下提供 Linux 排查命令。")
                .build();
    }

    /**
     * 基础分析接口
     */
    public String analyzeData(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }


    public String getDiagnosticReport(String metricContext, String currentAlert) {
        String systemPrompt = """
        你是一个资深的 AIOps 专家。我会为你提供系统近期的监控数据走势。
        请执行以下任务：
        1. 趋势预测：判断指标是【瞬时波动】还是【持续恶化】。
        2. 根因分析：结合当前告警的具体指标，分析可能的系统瓶颈（如内存泄漏、计算密集型任务、死循环等）。
        3. 处置建议：给出 1-2 条具体的排查命令（如 top, free -m, jmap 等）。
        请使用 Markdown 格式回答，保持专业简洁。
        """;

        String userPrompt = String.format("""
        【当前告警】：%s
        【历史背景】：
        %s
        """, currentAlert, metricContext);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Async // 关键：此方法调用将立即返回，在虚拟线程中执行
    public CompletableFuture<String> getDiagnosticReportAsync(String context, String alertInfo) {
        // 1. 检查冷却时间
        if (isCoolingDown()) {
            log.info("🧊 AI 诊断正处于冷却期，跳过本次深度分析...");
            return CompletableFuture.completedFuture("Skipped due to cooldown");
        }

        // 2. 更新最后调用时间
        this.lastCallTime = System.currentTimeMillis();

        long start = System.currentTimeMillis();
        String report = chatClient.prompt()
                .user("分析以下趋势并给出建议：" + context + " 告警内容：" + alertInfo)
                .call()
                .content();
        log.info("🕒 AI 诊断耗时: {} ms", System.currentTimeMillis() - start);
        return CompletableFuture.completedFuture(report);
    }

    public boolean isCoolingDown() {
        return (System.currentTimeMillis() - lastCallTime) < COOLDOWN_MS;
    }
}