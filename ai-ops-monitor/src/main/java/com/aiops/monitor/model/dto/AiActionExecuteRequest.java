package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiActionExecuteRequest {

    @Size(max = 20, message = "executionMode 最长 20 字符")
    private String executionMode;

    @Size(max = 20, message = "status 最长 20 字符")
    private String status;

    @Size(max = 20000, message = "outputText 最长 20000 字符")
    private String outputText;

    @Size(max = 5000, message = "errorMessage 最长 5000 字符")
    private String errorMessage;
}
