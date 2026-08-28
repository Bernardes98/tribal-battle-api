package com.tribalbattle.tribal_battle_api.intelligence.dto;

import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistorySource;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record IntelligenceReportResponse(
        UUID id,
        String clientId,
        SimulationHistorySource source,
        JsonNode payload,
        JsonNode result,
        JsonNode reportMetadata,
        boolean favorite,
        Instant createdAt,
        boolean ownedByAccount
) {
}
