package com.tribalbattle.tribal_battle_api.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        Instant expiresAt,
        AuthUserResponse user
) {
}
