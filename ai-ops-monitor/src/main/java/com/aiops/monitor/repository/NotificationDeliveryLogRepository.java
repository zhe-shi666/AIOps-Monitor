package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.NotificationDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {
    Page<NotificationDeliveryLog> findByIncidentId(Long incidentId, Pageable pageable);
}
