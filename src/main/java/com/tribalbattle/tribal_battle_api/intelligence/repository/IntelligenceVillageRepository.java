package com.tribalbattle.tribal_battle_api.intelligence.repository;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceVillage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntelligenceVillageRepository extends JpaRepository<IntelligenceVillage, UUID> {
    Optional<IntelligenceVillage> findByUserIdAndVillageKey(UUID userId, String villageKey);
    List<IntelligenceVillage> findByUserIdOrderByLastSeenAtDesc(UUID userId);
    List<IntelligenceVillage> findByUserIdAndPlayerIdOrderByLastSeenAtDesc(UUID userId, UUID playerId);
    List<IntelligenceVillage> findByUserIdAndVillageKeyIn(UUID userId, Collection<String> villageKeys);
}
