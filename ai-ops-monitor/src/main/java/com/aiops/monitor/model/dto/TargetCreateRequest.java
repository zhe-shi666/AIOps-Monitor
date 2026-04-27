package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TargetCreateRequest {
    @NotBlank(message = "name 不能为空")
    private String name;
    private String hostname;
    private String description;
}
