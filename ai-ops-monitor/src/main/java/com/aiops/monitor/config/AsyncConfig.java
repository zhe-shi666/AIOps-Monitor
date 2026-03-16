package com.aiops.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // 开启异步支持
public class AsyncConfig {
    // 开启 spring.threads.virtual.enabled 后，@Async 默认就会使用虚拟线程
}