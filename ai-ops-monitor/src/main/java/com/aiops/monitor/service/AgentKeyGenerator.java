package com.aiops.monitor.service;

import com.aiops.monitor.repository.MonitorTargetRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AgentKeyGenerator {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final int KEY_BYTES = 32;
    private static final int MAX_RETRY = 5;
    private final SecureRandom secureRandom = new SecureRandom();
    private final MonitorTargetRepository monitorTargetRepository;

    public AgentKeyGenerator(MonitorTargetRepository monitorTargetRepository) {
        this.monitorTargetRepository = monitorTargetRepository;
    }

    public String generateUniqueKey() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String key = generateHex();
            if (monitorTargetRepository.findByAgentKey(key).isEmpty()) {
                return key;
            }
        }
        throw new IllegalStateException("无法生成唯一 Agent Key，请重试");
    }

    private String generateHex() {
        byte[] bytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(bytes);
        char[] out = new char[KEY_BYTES * 2];
        for (int i = 0; i < KEY_BYTES; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
