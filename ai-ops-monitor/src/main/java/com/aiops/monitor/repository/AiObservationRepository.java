package com.aiops.monitor.repository;

import com.aiops.monitor.model.entity.AiObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiObservationRepository extends JpaRepository<AiObservation, Long> {

    List<AiObservation> findByInvestigationIdOrderByObservedAtAsc(Long investigationId);

    List<AiObservation> findByInvestigationIdAndUserIdOrderByObservedAtAsc(Long investigationId, Long userId);
}
