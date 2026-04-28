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

            // 3. 构建结构化数据行（全指标）
            sb.append(String.format(
                    "- [%s] 节点: **%s** | CPU: %.1f%% | MEM: %.1f%% | DISK: %.1f%% | RX: %.0fB/s | TX: %.0fB/s | PROC: %d\n",
                    timeStr,
                    node,
                    record.getCpuUsage(),
                    record.getMemUsage(),
                    record.getDiskUsage() == null ? 0d : record.getDiskUsage(),
                    record.getNetRxBytesPerSec() == null ? 0d : record.getNetRxBytesPerSec(),
                    record.getNetTxBytesPerSec() == null ? 0d : record.getNetTxBytesPerSec(),
                    record.getProcessCount() == null ? 0 : record.getProcessCount()
            ));
        }

        // 4. 补充分析指令引导（可选，能让 AI 输出更稳定）
        sb.append("\n请基于以上多维指标分析风险，重点识别：CPU/MEM/DISK 饱和、网络突刺、进程异常增长。")
                .append("请给出根因假设、排查命令、修复动作及回滚方案。");

        return sb.toString();
    }
}
