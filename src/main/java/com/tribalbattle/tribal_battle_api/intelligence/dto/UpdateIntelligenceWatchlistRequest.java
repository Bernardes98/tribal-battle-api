package com.tribalbattle.tribal_battle_api.intelligence.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateIntelligenceWatchlistRequest(
        @NotNull
        @Size(max = 500)
        List<@Size(max = 600) String> watchedVillageKeys,

        @Min(5)
        @Max(200)
        int alertThresholdPercent
) {
}
