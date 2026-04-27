package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiObservationCreateRequest {

    @NotBlank(message = "type 不能为空")
    @Size(max = 30, message = "type 最长 30 字符")
    private String type;

    @Size(max = 255, message = "sourceRef 最长 255 字符")
    private String sourceRef;

    @Size(max = 255, message = "hostname 最长 255 字符")
    private String hostname;

    @Size(max = 50, message = "metricName 最长 50 字符")
    private String metricName;

    private Double metricValue;

    private LocalDateTime observedAt;

    @DecimalMin(value = "0.0", message = "confidence 不能小于 0")
    @DecimalMax(value = "1.0", message = "confidence 不能大于 1")
    private Double confidence;

    @Size(max = 65535, message = "payloadJson 最大 65535 字符")
    private String payloadJson;
}
