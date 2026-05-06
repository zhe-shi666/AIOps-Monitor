package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AgentRelease;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentReleaseRepository extends JpaRepository<AgentRelease, Long> {
    Optional<AgentRelease> findFirstByActiveTrueOrderByCreatedAtDesc();
    Optional<AgentRelease> findByVersion(String version);
}
