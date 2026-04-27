package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationChannelEnabledRequest {
    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}
