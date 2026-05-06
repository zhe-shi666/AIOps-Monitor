package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlatformMailSettingsRequest {
    @NotBlank(message = "smtpHost 不能为空")
    @Size(max = 255, message = "smtpHost 长度不能超过 255")
    private String smtpHost;

    @Min(value = 1, message = "smtpPort 必须大于 0")
    @Max(value = 65535, message = "smtpPort 不能超过 65535")
    private Integer smtpPort = 587;

    @NotBlank(message = "smtpUsername 不能为空")
    @Size(max = 255, message = "smtpUsername 长度不能超过 255")
    private String smtpUsername;

    @Size(max = 500, message = "smtpPassword 长度不能超过 500")
    private String smtpPassword;

    private Boolean smtpAuth = true;

    private Boolean smtpStarttls = true;

    @NotBlank(message = "fromAddress 不能为空")
    @Email(message = "fromAddress 格式不合法")
    @Size(max = 255, message = "fromAddress 长度不能超过 255")
    private String fromAddress;

    @Size(max = 100, message = "fromName 长度不能超过 100")
    private String fromName;

    private Boolean enabled = true;
}
