package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationChannelRequest {
    @NotBlank(message = "name 不能为空")
    @Size(max = 100, message = "name 长度不能超过 100")
    private String name;

    @NotBlank(message = "type 不能为空")
    private String type;

    @NotBlank(message = "webhookUrl 不能为空")
    @Size(max = 500, message = "webhookUrl 长度不能超过 500")
    private String webhookUrl;

    @Size(max = 255, message = "secret 长度不能超过 255")
    private String secret;

    private Boolean enabled = true;
}
