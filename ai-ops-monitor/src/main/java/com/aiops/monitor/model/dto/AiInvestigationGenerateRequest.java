package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiInvestigationGenerateRequest {

    @Size(max = 1000, message = "promptHint 最长 1000 字符")
    private String promptHint;

    private Boolean includePostmortem;
}
