package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentHeartbeatRequest {
    @NotBlank(message = "agentKey 不能为空")
    private String agentKey;

    @NotBlank(message = "hostname 不能为空")
    private String hostname;

    @NotNull(message = "cpuUsage 不能为空")
    @DecimalMin(value = "0.0", message = "cpuUsage 不能小于 0")
    @DecimalMax(value = "100.0", message = "cpuUsage 不能大于 100")
    private Double cpuUsage;

    @NotNull(message = "memUsage 不能为空")
    @DecimalMin(value = "0.0", message = "memUsage 不能小于 0")
    @DecimalMax(value = "100.0", message = "memUsage 不能大于 100")
    private Double memUsage;

    private Long timestamp;
    private String ip;
    private String agentVersion;
}
