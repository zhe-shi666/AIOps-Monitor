package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IncidentStatusUpdateRequest {
    @NotBlank(message = "status 不能为空")
    private String status;
}
