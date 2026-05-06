package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.TargetNotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TargetNotificationSubscriptionRepository extends JpaRepository<TargetNotificationSubscription, Long> {
    Optional<TargetNotificationSubscription> findByTargetIdAndUserId(Long targetId, Long userId);
    boolean existsByTargetIdAndUserIdAndEnabledTrue(Long targetId, Long userId);
    long countByTargetIdAndEnabledTrue(Long targetId);
    List<TargetNotificationSubscription> findByTargetIdAndEnabledTrue(Long targetId);
    List<TargetNotificationSubscription> findByTargetIdInAndEnabledTrue(Collection<Long> targetIds);
    List<TargetNotificationSubscription> findByUserIdAndEnabledTrue(Long userId);
}
