package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TargetSubscriptionUpdateRequest {
    @NotNull
    private Boolean enabled;
}
