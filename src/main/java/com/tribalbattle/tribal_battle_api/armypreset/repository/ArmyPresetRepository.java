package com.tribalbattle.tribal_battle_api.armypreset.repository;

import com.tribalbattle.tribal_battle_api.armypreset.entity.ArmyPreset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArmyPresetRepository
        extends JpaRepository<ArmyPreset, UUID> {

    List<ArmyPreset>
    findTop100ByClientIdOrderByTypeAscUpdatedAtDesc(
            String clientId
    );

    Optional<ArmyPreset>
    findByIdAndClientId(
            UUID id,
            String clientId
    );
}
