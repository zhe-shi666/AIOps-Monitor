package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentRegisterRequest {
    @NotBlank(message = "agentKey 不能为空")
    private String agentKey;

    @NotBlank(message = "hostname 不能为空")
    private String hostname;

    private String ip;
    private String agentVersion;
}
