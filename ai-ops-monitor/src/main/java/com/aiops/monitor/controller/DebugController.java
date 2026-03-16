package com.aiops.monitor.controller;

import com.aiops.monitor.collector.PrometheusHttpClient;
import com.aiops.monitor.model.dto.PrometheusResponse;
import com.aiops.monitor.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private PrometheusHttpClient promClient;

    @Autowired
    private AiService aiService;

    @GetMapping("/cpu-trend")
    public String getTrend() {
        double avgLoad = promClient.getAvgCpuLoad5m();
        return "过去 5 分钟平均 CPU 负载: " + String.format("%.2f", avgLoad) + "%";
    }

    @GetMapping("/check-prometheus")
    public String checkPrometheus() {
        // 1. 定义一个最简单的 PromQL：查询指标 'up'
        // 'up' 是 Prometheus 自带指标，1 表示目标在线，0 表示离线
        String query = "up{job='spring-ai-ops'}";

        try {
            PrometheusResponse response = promClient.query(query);
            if (response != null && "success".equals(response.getStatus())) {
                return "连接成功！获取到数据点数量: " + response.getData().getResult().size();
            }
            return "连接成功但数据异常: " + response;
        } catch (Exception e) {
            return "连接失败，报错: " + e.getMessage();
        }
    }

    @GetMapping("/ai/test")
    public String testAi(@RequestParam(defaultValue = "你好，介绍一下你自己") String msg) {
        return aiService.analyzeData(msg);
    }

}