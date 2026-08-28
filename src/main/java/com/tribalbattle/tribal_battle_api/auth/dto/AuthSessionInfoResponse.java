package com.tribalbattle.tribal_battle_api.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthSessionInfoResponse(
        UUID id,
        String userAgent,
        Instant createdAt,
        Instant expiresAt,
        boolean current
) {
}
