package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlatformMailTestRequest {
    @NotBlank(message = "recipientEmail 不能为空")
    @Email(message = "recipientEmail 格式不合法")
    @Size(max = 255, message = "recipientEmail 长度不能超过 255")
    private String recipientEmail;
}
