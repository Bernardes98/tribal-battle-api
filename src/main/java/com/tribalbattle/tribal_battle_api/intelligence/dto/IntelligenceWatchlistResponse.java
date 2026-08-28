package com.tribalbattle.tribal_battle_api.intelligence.dto;

import java.util.List;

public record IntelligenceWatchlistResponse(
        List<String> watchedVillageKeys,
        int alertThresholdPercent
) {
}
