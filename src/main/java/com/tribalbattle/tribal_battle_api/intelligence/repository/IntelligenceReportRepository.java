package com.tribalbattle.tribal_battle_api.intelligence.repository;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntelligenceReportRepository extends JpaRepository<IntelligenceReport, UUID> {
    boolean existsBySimulationHistoryId(UUID simulationHistoryId);
    long countByUserId(UUID userId);
    long countByUserIdAndVillageId(UUID userId, UUID villageId);
    List<IntelligenceReport> findByUserIdOrderByObservedAtDesc(UUID userId, Pageable pageable);
}
