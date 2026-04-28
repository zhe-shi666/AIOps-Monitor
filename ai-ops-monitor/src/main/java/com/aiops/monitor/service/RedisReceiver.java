package com.aiops.monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class RedisReceiver {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    private final GenericJackson2JsonRedisSerializer redisSerializer = new GenericJackson2JsonRedisSerializer();

    // 这个方法会被 RedisMessageListenerContainer 调用
    public void receiveMessage(Object message) {
        log.debug("从 Redis 收到广播消息: {}", message);
        Object normalized = message;
        if (message instanceof Message redisMessage) {
            try {
                normalized = redisSerializer.deserialize(redisMessage.getBody());
            } catch (Exception ex) {
                log.debug("redis message deserialize failed: {}", ex.getMessage());
            }
        }
        forwardNormalized(normalized);
    }

    private void forwardNormalized(Object normalized) {
        // 兼容旧格式（仅 metrics payload）和新格式（topic + payload envelope）
        if (normalized instanceof Map<?, ?> map && map.containsKey("topic")) {
            Object topicObj = map.get("topic");
            Object payload = map.get("payload");
            String topic = topicObj == null ? "/topic/metrics" : String.valueOf(topicObj);
            messagingTemplate.convertAndSend(topic, payload);
            return;
        }
        messagingTemplate.convertAndSend("/topic/metrics", normalized);
    }
}
