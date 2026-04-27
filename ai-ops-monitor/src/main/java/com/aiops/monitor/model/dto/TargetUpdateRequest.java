package com.aiops.monitor.model.dto;

import lombok.Data;

@Data
public class TargetUpdateRequest {
    private String name;
    private String hostname;
    private String description;
    private Boolean enabled;
}
