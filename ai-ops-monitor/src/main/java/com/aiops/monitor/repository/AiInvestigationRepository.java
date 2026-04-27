package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiInvestigation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiInvestigationRepository extends JpaRepository<AiInvestigation, Long> {

    Page<AiInvestigation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AiInvestigation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status, Pageable pageable);

    Optional<AiInvestigation> findByIdAndUserId(Long id, Long userId);
}
