package com.tribalbattle.tribal_battle_api.intelligence.dto;

import java.time.Instant;
import java.util.UUID;

public record IntelligenceVillageResponse(
        UUID id,
        UUID playerId,
        String villageKey,
        String playerName,
        String villageName,
        Integer x,
        Integer y,
        long reportCount,
        Instant lastSeenAt
) {
}
