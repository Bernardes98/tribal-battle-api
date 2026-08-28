package com.tribalbattle.tribal_battle_api.cloud.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SaveCloudStateRequest(

        @Min(0)
        long expectedRevision,

        @NotNull
        Map<String, String> payload
) {
}
