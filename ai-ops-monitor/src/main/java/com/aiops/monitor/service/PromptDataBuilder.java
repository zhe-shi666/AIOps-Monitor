package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.SystemMetricsHistory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PromptDataBuilder {

    // 将历史趋势转化为文本描述
    public String buildMetricContext(List<SystemMetricsHistory> history) {
        StringBuilder sb = new StringBuilder("近 5 分钟系统指标趋势如下：\n");
        for (SystemMetricsHistory record : history) {
            sb.append(String.format("[%s] CPU: %.2f%%, 内存: %.2f%%\n",
                    record.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    record.getCpuUsage(),
                    record.getMemUsage()));
        }
        return sb.toString();
    }
}