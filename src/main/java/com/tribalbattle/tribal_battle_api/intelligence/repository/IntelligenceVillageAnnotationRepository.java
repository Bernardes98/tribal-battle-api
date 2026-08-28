package com.tribalbattle.tribal_battle_api.intelligence.repository;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceVillageAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntelligenceVillageAnnotationRepository extends JpaRepository<IntelligenceVillageAnnotation, UUID> {
    List<IntelligenceVillageAnnotation> findByUserId(UUID userId);
    Optional<IntelligenceVillageAnnotation> findByUserIdAndVillageId(UUID userId, UUID villageId);
    void deleteByUserIdAndVillageId(UUID userId, UUID villageId);
}
