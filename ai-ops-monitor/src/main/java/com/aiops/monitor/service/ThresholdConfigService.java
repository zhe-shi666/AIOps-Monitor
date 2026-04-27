package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AlertThresholdConfig;
import com.aiops.monitor.repository.AlertThresholdConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ThresholdConfigService {

    private final AlertThresholdConfigRepository alertThresholdConfigRepository;

    @Value("${monitor.threshold.cpu:70}")
    private double defaultCpuThreshold;

    @Value("${monitor.threshold.memory:90}")
    private double defaultMemoryThreshold;

    @Value("${monitor.threshold.disk:85}")
    private double defaultDiskThreshold;

    @Value("${monitor.threshold.process-count:400}")
    private int defaultProcessCountThreshold;

    public AlertThresholdConfig getOrCreateByUserId(Long userId) {
        return alertThresholdConfigRepository.findByUserId(userId)
                .orElseGet(() -> {
                    AlertThresholdConfig config = new AlertThresholdConfig();
                    config.setUserId(userId);
                    config.setCpuThreshold(defaultCpuThreshold);
                    config.setMemoryThreshold(defaultMemoryThreshold);
                    config.setDiskThreshold(defaultDiskThreshold);
                    config.setProcessCountThreshold(defaultProcessCountThreshold);
                    config.setCreatedAt(LocalDateTime.now());
                    config.setUpdatedAt(LocalDateTime.now());
                    return alertThresholdConfigRepository.save(config);
                });
    }

    public AlertThresholdConfig update(Long userId, Double cpu, Double memory, Double disk, Integer processCount) {
        AlertThresholdConfig config = getOrCreateByUserId(userId);
        if (cpu != null) {
            config.setCpuThreshold(cpu);
        }
        if (memory != null) {
            config.setMemoryThreshold(memory);
        }
        if (disk != null) {
            config.setDiskThreshold(disk);
        }
        if (processCount != null) {
            config.setProcessCountThreshold(processCount);
        }
        config.setUpdatedAt(LocalDateTime.now());
        return alertThresholdConfigRepository.save(config);
    }
}
