package com.tribalbattle.tribal_battle_api.intelligence.service;

import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligencePlayer;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceReport;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceVillage;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligencePlayerRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceReportRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceVillageRepository;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntelligenceIngestionService {

    private final IntelligencePlayerRepository playerRepository;
    private final IntelligenceVillageRepository villageRepository;
    private final IntelligenceReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void ingest(SimulationHistory history) {
        UUID userId = history.getUserId();

        if (
                userId == null ||
                history.getReportMetadataPayload() == null ||
                history.getReportMetadataPayload().isBlank() ||
                reportRepository.existsBySimulationHistoryId(history.getId())
        ) {
            return;
        }

        JsonNode metadata = readJson(history.getReportMetadataPayload());
        JsonNode defender = metadata == null ? null : metadata.get("defender");

        if (defender == null || defender.isNull()) {
            return;
        }

        String playerName = cleanText(defender.path("playerName").asText(null));
        String villageName = cleanText(defender.path("villageName").asText(null));
        Integer x = readCoordinate(defender, "x");
        Integer y = readCoordinate(defender, "y");

        if (
                playerName == null &&
                villageName == null &&
                (x == null || y == null)
        ) {
            return;
        }

        String normalizedPlayerValue = normalize(playerName);
        String normalizedPlayer = normalizedPlayerValue.isBlank()
                ? "unknown-player"
                : normalizedPlayerValue;

        String storedPlayerName = playerName == null ? "Unknown player" : playerName;

        IntelligencePlayer player =
                playerRepository
                        .findByUserIdAndNormalizedName(userId, normalizedPlayer)
                        .orElseGet(() ->
                                IntelligencePlayer.builder()
                                        .userId(userId)
                                        .name(storedPlayerName)
                                        .normalizedName(normalizedPlayer)
                                        .build()
                        );

        player.setName(storedPlayerName);
        player = playerRepository.save(player);
        UUID playerId = player.getId();

        String villageKey = buildVillageKey(
                normalizedPlayer,
                playerName,
                villageName,
                x,
                y
        );

        String storedVillageName = villageName == null ? "Unknown village" : villageName;
        Instant observedAt = history.getCreatedAt() == null ? Instant.now() : history.getCreatedAt();

        IntelligenceVillage village =
                villageRepository
                        .findByUserIdAndVillageKey(userId, villageKey)
                        .orElseGet(() ->
                                IntelligenceVillage.builder()
                                        .userId(userId)
                                        .playerId(playerId)
                                        .villageKey(villageKey)
                                        .name(storedVillageName)
                                        .normalizedName(normalize(storedVillageName))
                                        .x(x)
                                        .y(y)
                                        .lastSeenAt(observedAt)
                                        .build()
                        );

        village.setPlayerId(playerId);
        village.setName(storedVillageName);
        village.setNormalizedName(normalize(storedVillageName));
        village.setX(x);
        village.setY(y);
        if (
                village.getLastSeenAt() == null ||
                observedAt.isAfter(village.getLastSeenAt())
        ) {
            village.setLastSeenAt(observedAt);
        }
        village = villageRepository.save(village);

        IntelligenceReport report =
                IntelligenceReport.builder()
                        .userId(userId)
                        .villageId(village.getId())
                        .simulationHistoryId(history.getId())
                        .source(history.getSource())
                        .payload(history.getPayload())
                        .resultPayload(history.getResultPayload())
                        .reportMetadataPayload(history.getReportMetadataPayload())
                        .observedAt(observedAt)
                        .build();

        reportRepository.save(report);
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer readCoordinate(JsonNode party, String axis) {
        JsonNode coordinates = party.get("coordinates");
        if (coordinates == null || coordinates.isNull()) {
            return null;
        }

        JsonNode value = coordinates.get(axis);
        if (value == null || !value.canConvertToInt()) {
            return null;
        }

        return value.asInt();
    }

    private String buildVillageKey(
            String normalizedPlayer,
            String playerName,
            String villageName,
            Integer x,
            Integer y
    ) {
        String villagePart;

        if (x != null && y != null) {
            villagePart = x + "|" + y;
        } else {
            String normalizedVillage = normalize(villageName);
            String normalizedPartyPlayer = normalize(playerName);

            if (!normalizedPartyPlayer.isBlank() && !normalizedVillage.isBlank()) {
                villagePart = normalizedPartyPlayer + "|" + normalizedVillage;
            } else if (!normalizedPartyPlayer.isBlank()) {
                villagePart = normalizedPartyPlayer;
            } else if (!normalizedVillage.isBlank()) {
                villagePart = normalizedVillage;
            } else {
                villagePart = "unknown-village";
            }
        }

        return normalizedPlayer + "::" + villagePart;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalize(String value) {
        String cleaned = cleanText(value);
        return cleaned == null ? "" : cleaned.toLowerCase(Locale.ROOT);
    }
}
