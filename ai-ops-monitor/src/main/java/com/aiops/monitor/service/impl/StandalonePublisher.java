package com.aiops.monitor.service.impl;

import com.aiops.monitor.service.MetricsPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
/**
 * 条件装配：
 * 当配置文件中 monitor.mode 为 standalone 或没写时，Spring 会加载这个类
 */
@ConditionalOnProperty(name = "monitor.mode", havingValue = "standalone", matchIfMissing = true)
public class StandalonePublisher implements MetricsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public StandalonePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void send(String topic, Object data) {
        // 执行单机环境下的 STOMP 推送
        messagingTemplate.convertAndSend(topic, data);
    }
}