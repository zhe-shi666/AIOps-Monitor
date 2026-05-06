package com.aiops.monitor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetricDTO implements Serializable {
    // 建议实现 Serializable 接口，虽然 Jackson 序列化不需要，但这是分布式 DTO 的好习惯
    private static final long serialVersionUID = 1L;

    private String ip;       // 改为 String，存储如 "172.20.10.3"
    private String hostname; // 机器名，如 "shizhe-laptop"
    private String name;     // 指标名：CPU / MEMORY
    private Double value;    // 指标数值
    private long timestamp;  // 时间戳：System.currentTimeMillis()
    private Long userId;     // 指标归属用户
    private Long targetId;   // 指标归属监控目标
}
