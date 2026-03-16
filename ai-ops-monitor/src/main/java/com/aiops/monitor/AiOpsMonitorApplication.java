package com.aiops.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 必须添加此注解，否则 @Scheduled 不生效
public class AiOpsMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiOpsMonitorApplication.class, args);
    }
}