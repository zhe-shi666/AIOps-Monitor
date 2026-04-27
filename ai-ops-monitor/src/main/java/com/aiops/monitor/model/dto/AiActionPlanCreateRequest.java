package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiActionPlanCreateRequest {

    private Long hypothesisId;

    @NotBlank(message = "actionType 不能为空")
    @Size(max = 50, message = "actionType 最长 50 字符")
    private String actionType;

    @NotBlank(message = "title 不能为空")
    @Size(max = 255, message = "title 最长 255 字符")
    private String title;

    @Size(max = 5000, message = "commandText 最长 5000 字符")
    private String commandText;

    @Size(max = 500, message = "runbookRef 最长 500 字符")
    private String runbookRef;

    @Size(max = 20, message = "riskLevel 最长 20 字符")
    private String riskLevel;

    private Boolean requiresApproval;

    @Size(max = 5000, message = "rollbackPlan 最长 5000 字符")
    private String rollbackPlan;
}
