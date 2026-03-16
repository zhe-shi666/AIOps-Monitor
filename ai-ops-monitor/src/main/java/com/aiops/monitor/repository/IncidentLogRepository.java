package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.IncidentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentLogRepository extends JpaRepository<IncidentLog, Long> {
}