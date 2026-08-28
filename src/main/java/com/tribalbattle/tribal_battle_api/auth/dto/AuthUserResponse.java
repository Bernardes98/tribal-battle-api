package com.tribalbattle.tribal_battle_api.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String displayName,
        Instant createdAt
) {
}
