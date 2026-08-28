package com.tribalbattle.tribal_battle_api.simulationhistory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimSimulationHistoryRequest(

        @NotBlank
        @Size(max = 36)
        String clientId
) {
}
