package com.tribalbattle.tribal_battle_api.simulationhistory.repository;

import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationHistoryRepository
        extends JpaRepository<SimulationHistory, UUID> {

    /*
     * Kept for compatibility with existing tests / internal callers.
     * Account-aware service methods below do NOT use these methods for
     * anonymous access because account-owned rows must not leak through
     * the old clientId-only path.
     */
    List<SimulationHistory>
    findTop50ByClientIdOrderByCreatedAtDesc(
            String clientId
    );

    Optional<SimulationHistory>
    findByIdAndClientId(
            UUID id,
            String clientId
    );

    List<SimulationHistory>
    findTop50ByClientIdAndUserIdIsNullOrderByCreatedAtDesc(
            String clientId
    );

    Optional<SimulationHistory>
    findByIdAndClientIdAndUserIdIsNull(
            UUID id,
            String clientId
    );

    List<SimulationHistory>
    findTop50ByUserIdOrderByCreatedAtDesc(
            UUID userId
    );

    Optional<SimulationHistory>
    findByIdAndUserId(
            UUID id,
            UUID userId
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query(
            "update SimulationHistory history " +
            "set history.userId = :userId " +
            "where history.clientId = :clientId " +
            "and history.userId is null"
    )
    int claimAnonymousHistory(
            @Param("clientId")
            String clientId,

            @Param("userId")
            UUID userId
    );
}
