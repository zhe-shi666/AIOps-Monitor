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

    @Value("${monitor.threshold.consecutive-breach-count:2}")
    private int defaultConsecutiveBreachCount;

    @Value("${monitor.threshold.silence-seconds:180}")
    private int defaultSilenceSeconds;

    public AlertThresholdConfig getOrCreateByUserId(Long userId) {
        AlertThresholdConfig config = alertThresholdConfigRepository.findByUserId(userId)
                .orElseGet(() -> {
                    AlertThresholdConfig created = new AlertThresholdConfig();
                    created.setUserId(userId);
                    created.setCpuThreshold(defaultCpuThreshold);
                    created.setMemoryThreshold(defaultMemoryThreshold);
                    created.setDiskThreshold(defaultDiskThreshold);
                    created.setProcessCountThreshold(defaultProcessCountThreshold);
                    created.setConsecutiveBreachCount(defaultConsecutiveBreachCount);
                    created.setSilenceSeconds(defaultSilenceSeconds);
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return alertThresholdConfigRepository.save(created);
                });
        return patchDefaultValues(config);
    }

    public AlertThresholdConfig update(Long userId,
                                       Double cpu,
                                       Double memory,
                                       Double disk,
                                       Integer processCount,
                                       Integer consecutiveBreachCount,
                                       Integer silenceSeconds) {
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
        if (consecutiveBreachCount != null) {
            config.setConsecutiveBreachCount(consecutiveBreachCount);
        }
        if (silenceSeconds != null) {
            config.setSilenceSeconds(silenceSeconds);
        }
        config.setUpdatedAt(LocalDateTime.now());
        return alertThresholdConfigRepository.save(config);
    }

    private AlertThresholdConfig patchDefaultValues(AlertThresholdConfig config) {
        boolean changed = false;
        if (config.getConsecutiveBreachCount() == null || config.getConsecutiveBreachCount() < 1) {
            config.setConsecutiveBreachCount(defaultConsecutiveBreachCount);
            changed = true;
        }
        if (config.getSilenceSeconds() == null || config.getSilenceSeconds() < 10) {
            config.setSilenceSeconds(defaultSilenceSeconds);
            changed = true;
        }
        if (changed) {
            config.setUpdatedAt(LocalDateTime.now());
            return alertThresholdConfigRepository.save(config);
        }
        return config;
    }
}
