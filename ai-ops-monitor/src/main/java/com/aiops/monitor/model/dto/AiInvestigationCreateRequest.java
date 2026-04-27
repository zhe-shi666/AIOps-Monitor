package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiInvestigationCreateRequest {

    private Long incidentId;
    private Long targetId;

    @Size(max = 200, message = "title 最长 200 字符")
    private String title;

    @Size(max = 30, message = "triggerSource 最长 30 字符")
    private String triggerSource;

    @Size(max = 10, message = "severity 最长 10 字符")
    private String severity;

    @Size(max = 5000, message = "summary 最长 5000 字符")
    private String summary;
}
