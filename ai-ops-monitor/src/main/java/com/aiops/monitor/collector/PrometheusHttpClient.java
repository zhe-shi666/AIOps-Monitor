package com.aiops.monitor.collector;

import com.aiops.monitor.model.dto.PrometheusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
public class PrometheusHttpClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PROMETHEUS_API = "http://localhost:9090/api/v1/query";

    public PrometheusResponse query(String promQL) {
        try {
            // 使用 .build().encode().toUri() 确保 [5m] 被转义为 %5B5m%5D
            URI uri = UriComponentsBuilder.fromHttpUrl(PROMETHEUS_API)
                    .queryParam("query", promQL)
                    .build()
                    .encode()
                    .toUri();

            log.info("正在请求 Prometheus URL: {}", uri); // 打印出来看一眼
            return restTemplate.getForObject(uri, PrometheusResponse.class);
        } catch (Exception e) {
            log.error("Prometheus 查询失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 新增：专门从响应中提取第一个标量值
     */
    private double extractValue(PrometheusResponse response) {
        if (response != null && "success".equals(response.getStatus())
                && response.getData() != null
                && !response.getData().getResult().isEmpty()) {

            // Prometheus 返回的格式通常是 [timestamp, "value"]
            // 拿到 result 列表中的第一个元素，再拿到 value 列表中的第二个元素（索引为1）
            Object valObj = response.getData().getResult().get(0).getValue().get(1);
            return Double.parseDouble(valObj.toString());
        }
        return 0.0;
    }

    public double getAvgCpuLoad5m() {
        // 1. 确保这里的 [5m] 是英文半角符号
        // 2. 确保指标名 os_cpu_usage 与 Actuator 中完全一致
        String q = "avg_over_time(os_cpu_usage[5m])";

        // 注意：不要手动在 URL 后面拼接 ?query=...
        // 使用 UriComponentsBuilder 可以自动处理编码问题
        PrometheusResponse response = query(q);
        return extractValue(response);
    }
}