package com.tribalbattle.tribal_battle_api.cloud.dto;

import java.time.Instant;
import java.util.Map;

public record CloudStateResponse(
        long revision,
        Instant updatedAt,
        Map<String, String> payload
) {
}
