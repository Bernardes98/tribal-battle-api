package com.tribalbattle.tribal_battle_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank
        @Size(
                min = 8,
                max = 72
        )
        String currentPassword,

        @NotBlank
        @Size(
                min = 8,
                max = 72
        )
        String newPassword
) {
}
