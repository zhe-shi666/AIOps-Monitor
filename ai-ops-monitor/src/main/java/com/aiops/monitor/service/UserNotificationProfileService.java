package com.aiops.monitor.service;

import com.aiops.monitor.model.dto.NotificationProfileRequest;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserNotificationProfileService {

    private final UserRepository userRepository;

    public User updateProfile(Long userId, NotificationProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setNotificationEmail(normalize(request.getRecipientEmail()));
        user.setNotificationEnabled(request.getEnabled() == null || request.getEnabled());
        return userRepository.save(user);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientEmail 不能为空");
        }
        return value.trim();
    }
}
