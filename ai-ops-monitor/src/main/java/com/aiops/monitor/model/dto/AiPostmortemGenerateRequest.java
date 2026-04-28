package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiPostmortemGenerateRequest {

    @Size(max = 1000, message = "additionalContext 最长 1000 字符")
    private String additionalContext;
}
