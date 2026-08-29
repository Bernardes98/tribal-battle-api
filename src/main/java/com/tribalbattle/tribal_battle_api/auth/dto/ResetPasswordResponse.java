package com.tribalbattle.tribal_battle_api.auth.dto;

public record ResetPasswordResponse(
        long revokedSessions,
        String message
) {
}
