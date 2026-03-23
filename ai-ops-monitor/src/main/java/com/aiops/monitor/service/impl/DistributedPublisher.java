package com.aiops.monitor.service.impl;

import com.aiops.monitor.service.MetricsPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
        // 将数据发布到 Redis 频道，频道名可以根据 topic 转换
        String channel = topic.replace("/", "_");
        redisTemplate.convertAndSend(channel, data);
    }
}