package com.aiops.monitor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private boolean passwordChangeRequired;
    private String notificationEmail;
    private boolean notificationEnabled;
}
