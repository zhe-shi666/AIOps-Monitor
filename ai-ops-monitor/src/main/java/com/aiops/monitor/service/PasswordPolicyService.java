package com.aiops.monitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordPolicyService {
    @Value("${security.password.min-length:10}")
    private int minLength;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${security.password.require-special:true}")
    private boolean requireSpecial;

    public void validate(String password) {
        if (password == null || password.length() < minLength) {
            throw invalid("密码长度至少 " + minLength + " 位");
        }
        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            throw invalid("密码必须包含大写字母");
        }
        if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
            throw invalid("密码必须包含小写字母");
        }
        if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
            throw invalid("密码必须包含数字");
        }
        if (requireSpecial && password.chars().allMatch(Character::isLetterOrDigit)) {
            throw invalid("密码必须包含特殊字符");
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
