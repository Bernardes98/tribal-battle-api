package com.tribalbattle.tribal_battle_api.armypreset.dto;

import com.tribalbattle.tribal_battle_api.armypreset.entity.ArmyPresetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CreateArmyPresetRequest(
        @NotBlank
        String clientId,

        @NotBlank
        @Size(max = 80)
        String name,

        @NotNull
        ArmyPresetType type,

        @NotNull
        JsonNode army,

        JsonNode context
) {
}
