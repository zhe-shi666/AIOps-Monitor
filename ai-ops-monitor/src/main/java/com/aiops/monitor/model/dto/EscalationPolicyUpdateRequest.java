package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EscalationPolicyUpdateRequest {

    @NotBlank(message = "p1Intervals 不能为空")
    @Size(max = 100, message = "p1Intervals 长度不能超过 100")
    private String p1Intervals;

    @NotBlank(message = "p2Intervals 不能为空")
    @Size(max = 100, message = "p2Intervals 长度不能超过 100")
    private String p2Intervals;

    @NotBlank(message = "p3Intervals 不能为空")
    @Size(max = 100, message = "p3Intervals 长度不能超过 100")
    private String p3Intervals;
}
