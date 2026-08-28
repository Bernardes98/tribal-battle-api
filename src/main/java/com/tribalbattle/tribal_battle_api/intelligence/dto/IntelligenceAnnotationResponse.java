package com.tribalbattle.tribal_battle_api.intelligence.dto;

import java.time.Instant;
import java.util.List;

public record IntelligenceAnnotationResponse(
        String villageKey,
        List<String> tags,
        String note,
        Instant updatedAt
) {
}
