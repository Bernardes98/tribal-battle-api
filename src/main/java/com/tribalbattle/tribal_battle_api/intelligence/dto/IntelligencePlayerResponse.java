package com.tribalbattle.tribal_battle_api.intelligence.dto;

import java.time.Instant;
import java.util.UUID;

public record IntelligencePlayerResponse(
        UUID id,
        String name,
        long villageCount,
        long reportCount,
        Instant lastSeenAt
) {
}
