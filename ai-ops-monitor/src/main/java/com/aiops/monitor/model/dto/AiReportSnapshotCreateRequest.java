package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiReportSnapshotCreateRequest {

    @Size(max = 20, message = "format 最长 20 字符")
    private String format;

    @Size(max = 100000, message = "reportMarkdown 最长 100000 字符")
    private String reportMarkdown;

    @Size(max = 200000, message = "reportJson 最长 200000 字符")
    private String reportJson;

    @Size(max = 100, message = "createdBy 最长 100 字符")
    private String createdBy;
}
