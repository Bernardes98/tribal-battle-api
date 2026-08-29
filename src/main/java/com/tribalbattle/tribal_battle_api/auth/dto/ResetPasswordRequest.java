package com.tribalbattle.tribal_battle_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Size(min = 32, max = 512)
        String token,

        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
