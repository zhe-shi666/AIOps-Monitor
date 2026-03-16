package com.aiops.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 注意：这里改成了 MessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 前端连接地址: http://localhost:8080/ws-monitor
        registry.addEndpoint("/ws-monitor")
                .setAllowedOriginPatterns("*") // 允许跨域
                .withSockJS(); // 支持 SockJS 回退
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 前端订阅的前缀，例如订阅 /topic/ai-reports
        registry.enableSimpleBroker("/topic");
        // 前端发送消息的前缀（可选）
        registry.setApplicationDestinationPrefixes("/app");
    }
}