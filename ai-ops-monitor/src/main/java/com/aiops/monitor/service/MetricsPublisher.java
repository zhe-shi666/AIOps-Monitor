package com.aiops.monitor.service;

/**
 * 消息推送统一接口（策略模式核心）
 */
public interface MetricsPublisher {
    /**
     * 发送指标数据
     * @param topic 订阅话题 (如 /topic/metrics)
     * @param data  推送的数据对象
     */
    void send(String topic, Object data);
}