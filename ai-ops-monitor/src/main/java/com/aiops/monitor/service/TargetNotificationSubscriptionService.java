package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.TargetNotificationSubscription;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.TargetNotificationSubscriptionRepository;
import com.aiops.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TargetNotificationSubscriptionService {

    private final TargetNotificationSubscriptionRepository repository;
    private final UserRepository userRepository;

    public TargetNotificationSubscription ensureSubscribed(Long targetId, Long userId) {
        return repository.findByTargetIdAndUserId(targetId, userId)
                .map(existing -> {
                    if (!existing.isEnabled()) {
                        existing.setEnabled(true);
                        existing.setUpdatedAt(LocalDateTime.now());
                        return repository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    TargetNotificationSubscription created = new TargetNotificationSubscription();
                    created.setTargetId(targetId);
                    created.setUserId(userId);
                    created.setEnabled(true);
                    created.setCreatedAt(LocalDateTime.now());
                    created.setUpdatedAt(LocalDateTime.now());
                    return repository.save(created);
                });
    }

    public TargetNotificationSubscription setSubscription(Long targetId, Long userId, boolean enabled) {
        TargetNotificationSubscription subscription = repository.findByTargetIdAndUserId(targetId, userId)
                .orElseGet(() -> {
                    TargetNotificationSubscription created = new TargetNotificationSubscription();
                    created.setTargetId(targetId);
                    created.setUserId(userId);
                    created.setCreatedAt(LocalDateTime.now());
                    return created;
                });
        subscription.setEnabled(enabled);
        subscription.setUpdatedAt(LocalDateTime.now());
        return repository.save(subscription);
    }

    public boolean isSubscribed(Long targetId, Long userId) {
        return repository.existsByTargetIdAndUserIdAndEnabledTrue(targetId, userId);
    }

    public long countSubscribers(Long targetId) {
        return repository.countByTargetIdAndEnabledTrue(targetId);
    }

    public List<TargetNotificationSubscription> listSubscribers(Long targetId) {
        return repository.findByTargetIdAndEnabledTrue(targetId);
    }

    public Set<Long> listSubscribedTargetIds(Long userId) {
        return new LinkedHashSet<>(repository.findByUserIdAndEnabledTrue(userId)
                .stream()
                .map(TargetNotificationSubscription::getTargetId)
                .toList());
    }

    public List<User> listSubscriberUsers(Long targetId) {
        List<Long> userIds = repository.findByTargetIdAndEnabledTrue(targetId)
                .stream()
                .map(TargetNotificationSubscription::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(userIds);
    }
}
