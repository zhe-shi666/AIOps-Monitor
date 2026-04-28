package com.aiops.monitor.service.impl;

import com.aiops.monitor.service.MetricsPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
// 只有当配置为 monitor.mode: distributed 时才激活
@ConditionalOnProperty(name = "monitor.mode", havingValue = "distributed")
public class DistributedPublisher implements MetricsPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public DistributedPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void send(String topic, Object data) {
        // 将 topic 和 payload 封装后发布，便于多频道复用
        String channel = topic.replace("/", "_");
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("topic", topic);
        envelope.put("payload", data);
        redisTemplate.convertAndSend(channel, envelope);
    }
}
