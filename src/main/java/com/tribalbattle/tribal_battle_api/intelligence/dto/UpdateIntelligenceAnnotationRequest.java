package com.tribalbattle.tribal_battle_api.intelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateIntelligenceAnnotationRequest(
        @NotBlank
        @Size(max = 600)
        String villageKey,

        @NotNull
        @Size(max = 20)
        List<@Size(max = 64) String> tags,

        @NotNull
        @Size(max = 1000)
        String note
) {
}
