package com.tribalbattle.tribal_battle_api.armypreset.dto;

import com.tribalbattle.tribal_battle_api.armypreset.entity.ArmyPresetType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ArmyPresetResponse(
        UUID id,
        String clientId,
        String name,
        ArmyPresetType type,
        JsonNode army,
        JsonNode context,
        Instant createdAt,
        Instant updatedAt
) {
}
