package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentLogItem {
    @NotBlank(message = "level 不能为空")
    private String level;

    @NotBlank(message = "message 不能为空")
    private String message;

    private Long timestamp;
    private String traceId;
    private String labelsJson;
}
