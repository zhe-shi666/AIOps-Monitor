package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.PlatformMailSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformMailSettingsRepository extends JpaRepository<PlatformMailSettings, Long> {
    Optional<PlatformMailSettings> findFirstByOrderByIdAsc();
}
