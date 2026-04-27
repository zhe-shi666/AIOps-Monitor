package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AlertEscalationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertEscalationPolicyRepository extends JpaRepository<AlertEscalationPolicy, Long> {
    Optional<AlertEscalationPolicy> findByUserId(Long userId);
}
