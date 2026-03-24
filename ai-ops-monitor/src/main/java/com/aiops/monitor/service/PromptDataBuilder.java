package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.SystemMetricsHistory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PromptDataBuilder {

    /**
     * 构建指标上下文
     * @param history 指标历史列表
     * @param isDistributed 是否为分布式模式
     */
    public String buildMetricContext(List<SystemMetricsHistory> history, boolean isDistributed) {
        // 1. 根据模式动态生成标题
        String title = isDistributed ? "🌐 全集群整体指标监控" : "🖥️ 单节点本地指标监控";
        StringBuilder sb = new StringBuilder("### " + title + " (近 5 分钟数据快照)\n");

        if (history == null || history.isEmpty()) {
            return sb.append("暂无历史数据。").toString();
        }

        // 2. 遍历历史记录
        for (SystemMetricsHistory record : history) {
            // 获取节点名称，如果为空则填充默认值
            String node = record.getHostname() != null ? record.getHostname() : "Unknown-Node";

            // 格式化时间
            String timeStr = record.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 3. 构建结构化数据行
            // 增加节点标识是非常关键的，尤其是在分布式模式下，AI 需要根据 hostname 来判断是哪个节点在报警
            sb.append(String.format("- [%s] 节点: **%s** | CPU 使用率: %.1f%% | 内存占用: %.1f%%\n",
                    timeStr,
                    node,
                    record.getCpuUsage(),
                    record.getMemUsage()));
        }

        // 4. 补充分析指令引导（可选，能让 AI 输出更稳定）
        sb.append("\n请基于以上数据，分析系统负载趋势。如果存在 CPU 持续波动或内存溢出风险，请指出疑似节点并提供修复建议。");

        return sb.toString();
    }
}