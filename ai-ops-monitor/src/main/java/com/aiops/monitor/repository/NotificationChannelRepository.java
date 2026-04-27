package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {
    List<NotificationChannel> findByUserIdOrderByIdDesc(Long userId);

    List<NotificationChannel> findByUserIdAndEnabledTrueOrderByIdDesc(Long userId);

    Optional<NotificationChannel> findByIdAndUserId(Long id, Long userId);
}
