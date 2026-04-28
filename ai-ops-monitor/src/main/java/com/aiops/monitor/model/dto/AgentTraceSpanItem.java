package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentTraceSpanItem {
    @NotBlank(message = "traceId 不能为空")
    private String traceId;

    private String spanId;
    private String parentSpanId;
    private String operationName;

    @NotNull(message = "durationMs 不能为空")
    private Long durationMs;

    private String status;
    private String attributesJson;
    private Long timestamp;
}
