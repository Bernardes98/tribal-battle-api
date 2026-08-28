package com.tribalbattle.tribal_battle_api.intelligence.repository;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceWatchlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntelligenceWatchlistEntryRepository extends JpaRepository<IntelligenceWatchlistEntry, UUID> {
    List<IntelligenceWatchlistEntry> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
