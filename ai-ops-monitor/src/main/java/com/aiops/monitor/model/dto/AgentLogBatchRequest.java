package com.aiops.monitor.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AgentLogBatchRequest {
    @NotBlank(message = "agentKey 不能为空")
    private String agentKey;

    private String serviceName;
    private String source;
    private String hostname;

    @Valid
    @NotEmpty(message = "logs 不能为空")
    private List<AgentLogItem> logs;
}
