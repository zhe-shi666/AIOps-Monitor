package com.aiops.monitor.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemMetrics {
    private double cpuUsage;
    private double memoryUsage;
    private long uptime;
}