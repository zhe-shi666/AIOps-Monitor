package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.SystemMetricsHistory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PromptDataBuilder {

    public String buildMetricContext(List<SystemMetricsHistory> history) {
        StringBuilder sb = new StringBuilder("### 🌐 全集群指标分析（近 5 分钟）\n");

        // 建议在数据库查询时按时间倒序
        for (SystemMetricsHistory record : history) {
            // 增加 hostname 展示，让 AI 区分节点
            String node = record.getHostname() != null ? record.getHostname() : "未知节点";
            sb.append(String.format("- [%s] **%s** -> CPU: %.1f%%, MEM: %.1f%%\n",
                    record.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    node,
                    record.getCpuUsage(),
                    record.getMemUsage()));
        }
        return sb.toString();
    }
}