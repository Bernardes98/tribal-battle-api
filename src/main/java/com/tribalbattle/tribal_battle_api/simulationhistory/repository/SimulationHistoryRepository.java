package com.tribalbattle.tribal_battle_api.simulationhistory.repository;

import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationHistoryRepository
        extends JpaRepository<SimulationHistory, UUID> {

    List<SimulationHistory>
    findTop50ByClientIdOrderByCreatedAtDesc(
            String clientId
    );

    Optional<SimulationHistory>
    findByIdAndClientId(
            UUID id,
            String clientId
    );
}
