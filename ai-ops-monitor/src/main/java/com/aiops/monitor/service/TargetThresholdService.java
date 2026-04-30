package com.aiops.monitor.service;

import com.aiops.monitor.model.dto.TargetThresholdUpdateRequest;
import com.aiops.monitor.model.entity.AlertThresholdConfig;
import com.aiops.monitor.model.entity.TargetThresholdConfig;
import com.aiops.monitor.repository.TargetThresholdConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TargetThresholdService {
    private final ThresholdConfigService thresholdConfigService;
    private final TargetThresholdConfigRepository targetThresholdConfigRepository;

    public EffectiveThreshold resolve(Long userId, Long targetId) {
        AlertThresholdConfig userConfig = thresholdConfigService.getOrCreateByUserId(userId);
        TargetThresholdConfig targetConfig = targetId == null ? null : targetThresholdConfigRepository
                .findByUserIdAndTargetId(userId, targetId)
                .filter(TargetThresholdConfig::isEnabled)
                .orElse(null);
        return new EffectiveThreshold(
                pick(targetConfig == null ? null : targetConfig.getCpuThreshold(), userConfig.getCpuThreshold()),
                pick(targetConfig == null ? null : targetConfig.getMemoryThreshold(), userConfig.getMemoryThreshold()),
                pick(targetConfig == null ? null : targetConfig.getDiskThreshold(), userConfig.getDiskThreshold()),
                pick(targetConfig == null ? null : targetConfig.getProcessCountThreshold(), userConfig.getProcessCountThreshold()),
                pick(targetConfig == null ? null : targetConfig.getConsecutiveBreachCount(), userConfig.getConsecutiveBreachCount()),
                pick(targetConfig == null ? null : targetConfig.getSilenceSeconds(), userConfig.getSilenceSeconds()),
                targetConfig != null
        );
    }

    public TargetThresholdConfig getOrCreate(Long userId, Long targetId) {
        return targetThresholdConfigRepository.findByUserIdAndTargetId(userId, targetId)
                .orElseGet(() -> {
                    TargetThresholdConfig created = new TargetThresholdConfig();
                    created.setUserId(userId);
                    created.setTargetId(targetId);
                    created.setEnabled(false);
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return targetThresholdConfigRepository.save(created);
                });
    }

    public TargetThresholdConfig update(Long userId, Long targetId, TargetThresholdUpdateRequest request) {
        TargetThresholdConfig config = getOrCreate(userId, targetId);
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());
        if (request.getCpuThreshold() != null) config.setCpuThreshold(request.getCpuThreshold());
        if (request.getMemoryThreshold() != null) config.setMemoryThreshold(request.getMemoryThreshold());
        if (request.getDiskThreshold() != null) config.setDiskThreshold(request.getDiskThreshold());
        if (request.getProcessCountThreshold() != null) config.setProcessCountThreshold(request.getProcessCountThreshold());
        if (request.getConsecutiveBreachCount() != null) config.setConsecutiveBreachCount(request.getConsecutiveBreachCount());
        if (request.getSilenceSeconds() != null) config.setSilenceSeconds(request.getSilenceSeconds());
        config.setUpdatedAt(LocalDateTime.now());
        return targetThresholdConfigRepository.save(config);
    }

    public Map<String, Object> toView(Long userId, Long targetId) {
        TargetThresholdConfig config = getOrCreate(userId, targetId);
        EffectiveThreshold effective = resolve(userId, targetId);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("enabled", config.isEnabled());
        view.put("cpuThreshold", config.getCpuThreshold());
        view.put("memoryThreshold", config.getMemoryThreshold());
        view.put("diskThreshold", config.getDiskThreshold());
        view.put("processCountThreshold", config.getProcessCountThreshold());
        view.put("consecutiveBreachCount", config.getConsecutiveBreachCount());
        view.put("silenceSeconds", config.getSilenceSeconds());
        view.put("effectiveCpuThreshold", effective.cpuThreshold());
        view.put("effectiveMemoryThreshold", effective.memoryThreshold());
        view.put("effectiveDiskThreshold", effective.diskThreshold());
        view.put("effectiveProcessCountThreshold", effective.processCountThreshold());
        view.put("effectiveConsecutiveBreachCount", effective.consecutiveBreachCount());
        view.put("effectiveSilenceSeconds", effective.silenceSeconds());
        view.put("updatedAt", config.getUpdatedAt());
        return view;
    }

    private Double pick(Double targetValue, Double fallback) { return targetValue == null ? fallback : targetValue; }
    private Integer pick(Integer targetValue, Integer fallback) { return targetValue == null ? fallback : targetValue; }

    public record EffectiveThreshold(Double cpuThreshold,
                                     Double memoryThreshold,
                                     Double diskThreshold,
                                     Integer processCountThreshold,
                                     Integer consecutiveBreachCount,
                                     Integer silenceSeconds,
                                     boolean targetOverrideEnabled) {}
}
