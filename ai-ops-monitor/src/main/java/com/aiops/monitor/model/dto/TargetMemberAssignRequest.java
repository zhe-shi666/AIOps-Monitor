package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TargetMemberAssignRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Boolean enabled;
}
