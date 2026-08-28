package com.tribalbattle.tribal_battle_api.intelligence.repository;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceWatchlistSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IntelligenceWatchlistSettingsRepository extends JpaRepository<IntelligenceWatchlistSettings, UUID> {
}
