package com.tribalbattle.tribal_battle_api.cloud.dto;

import jakarta.validation.constraints.Min;

public record RestoreCloudStateRequest(

        @Min(0)
        long expectedRevision
) {
}
