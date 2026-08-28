package com.tribalbattle.tribal_battle_api.simulationhistory.dto;

import java.util.List;

public record SimulationHistoryPageResponse(
        List<SimulationHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
