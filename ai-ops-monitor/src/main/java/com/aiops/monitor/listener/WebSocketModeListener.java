package com.aiops.monitor.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketModeListener {

    private final SimpMessagingTemplate messagingTemplate;

    // 从启动参数 --monitor.mode 中读取，默认是 standalone
    @Value("${monitor.mode:standalone}")
    private String monitorMode;

    public WebSocketModeListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        // 当用户订阅 /topic/system-status 时，触发模式推送
        // 使用线程异步，确保前端已经完成订阅准备
        new Thread(() -> {
            try {
                Thread.sleep(800); // 给前端 0.8秒 的缓冲时间
                Map<String, String> status = new HashMap<>();
                status.put("mode", monitorMode);

                messagingTemplate.convertAndSend("/topic/system-status", status);
                System.out.println("成功向新客户端同步运行模式: " + monitorMode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}