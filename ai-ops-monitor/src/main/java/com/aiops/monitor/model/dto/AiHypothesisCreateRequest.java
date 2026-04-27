package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiHypothesisCreateRequest {

    @NotBlank(message = "title 不能为空")
    @Size(max = 255, message = "title 最长 255 字符")
    private String title;

    @Size(max = 5000, message = "reasoning 最长 5000 字符")
    private String reasoning;

    @DecimalMin(value = "0.0", message = "confidence 不能小于 0")
    @DecimalMax(value = "1.0", message = "confidence 不能大于 1")
    private Double confidence;

    private Integer rankOrder;

    @Size(max = 20, message = "status 最长 20 字符")
    private String status;
}
