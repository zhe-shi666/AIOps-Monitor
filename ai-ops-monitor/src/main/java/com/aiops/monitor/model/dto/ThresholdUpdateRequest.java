package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ThresholdUpdateRequest {
    @Min(value = 1, message = "cpuThreshold 不能小于 1")
    @Max(value = 100, message = "cpuThreshold 不能大于 100")
    private Double cpuThreshold;

    @Min(value = 1, message = "memoryThreshold 不能小于 1")
    @Max(value = 100, message = "memoryThreshold 不能大于 100")
    private Double memoryThreshold;

    @Min(value = 1, message = "diskThreshold 不能小于 1")
    @Max(value = 100, message = "diskThreshold 不能大于 100")
    private Double diskThreshold;

    @Min(value = 1, message = "processCountThreshold 不能小于 1")
    @Max(value = 100000, message = "processCountThreshold 超出范围")
    private Integer processCountThreshold;
}
