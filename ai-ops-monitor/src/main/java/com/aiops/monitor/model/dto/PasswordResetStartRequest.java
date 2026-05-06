package com.aiops.monitor.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class PasswordResetStartRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;
}
