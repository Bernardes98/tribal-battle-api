package com.tribalbattle.tribal_battle_api.simulationhistory.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSimulationHistoryFavoriteRequest(
        @NotNull
        Boolean favorite
) {
}
