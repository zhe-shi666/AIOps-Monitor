package com.aiops.monitor.collector;

import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.repository.MonitorTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TargetHeartbeatMonitorTask {

    private final MonitorTargetRepository monitorTargetRepository;

    @Value("${monitor.target.offline-seconds:90}")
    private long offlineSeconds;

    @Scheduled(fixedRate = 30000)
    public void refreshOfflineTargets() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(offlineSeconds);
        List<MonitorTarget> staleTargets = monitorTargetRepository.findByEnabledTrueAndLastHeartbeatAtBefore(threshold);
        if (staleTargets.isEmpty()) {
            return;
        }

        for (MonitorTarget target : staleTargets) {
            if (!"OFFLINE".equalsIgnoreCase(target.getStatus())) {
                target.setStatus("OFFLINE");
            }
        }
        monitorTargetRepository.saveAll(staleTargets);
        log.debug("检测到 {} 个目标心跳超时，已置为 OFFLINE", staleTargets.size());
    }
}
