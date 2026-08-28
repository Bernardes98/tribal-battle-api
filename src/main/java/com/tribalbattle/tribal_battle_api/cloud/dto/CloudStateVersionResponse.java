package com.tribalbattle.tribal_battle_api.cloud.dto;

import java.time.Instant;

public record CloudStateVersionResponse(
        long revision,
        Instant snapshotAt,
        boolean current
) {
}
