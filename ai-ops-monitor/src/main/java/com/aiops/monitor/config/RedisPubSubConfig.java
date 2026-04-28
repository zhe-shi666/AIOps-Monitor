package com.aiops.monitor.config;

import com.aiops.monitor.service.RedisReceiver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 订阅所有 _topic_* 频道（可转发 metrics、ai-reports、investigations 等）
        container.addMessageListener(listenerAdapter, new PatternTopic("_topic_*"));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisReceiver receiver) {
        // 指定接收类和处理方法名
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
