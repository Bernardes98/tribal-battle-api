package com.tribalbattle.tribal_battle_api.simulationhistory.dto;

import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistorySource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CreateSimulationHistoryRequest(
        @NotBlank
        @Size(max = 36)
        String clientId,

        @NotNull
        SimulationHistorySource source,

        @NotNull
        JsonNode payload,

        JsonNode result
) {
}
