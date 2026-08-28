package com.tribalbattle.tribal_battle_api.intelligence.repository;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligencePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntelligencePlayerRepository extends JpaRepository<IntelligencePlayer, UUID> {
    Optional<IntelligencePlayer> findByUserIdAndNormalizedName(UUID userId, String normalizedName);
    List<IntelligencePlayer> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
