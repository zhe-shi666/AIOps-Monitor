package com.aiops.monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisReceiver {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 这个方法会被 RedisMessageListenerContainer 调用
    public void receiveMessage(Object message) {
        log.debug("从 Redis 收到广播消息: {}", message);
        // 将消息通过 WebSocket 转发给前端
        // 注意：分布式环境下，每个节点都会执行这个动作
        messagingTemplate.convertAndSend("/topic/metrics", message);
    }
}