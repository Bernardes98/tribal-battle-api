package com.tribalbattle.tribal_battle_api.simulationhistory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkDeleteSimulationHistoryRequest(
        @NotEmpty
        @Size(max = 100)
        List<UUID> ids
) {
}
