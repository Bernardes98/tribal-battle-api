package com.tribalbattle.tribal_battle_api.intelligence.service;

import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceAnnotationResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligencePlayerResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceReportResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceVillageResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceWatchlistResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.UpdateIntelligenceAnnotationRequest;
import com.tribalbattle.tribal_battle_api.intelligence.dto.UpdateIntelligenceWatchlistRequest;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligencePlayer;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceReport;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceVillage;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceVillageAnnotation;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceWatchlistEntry;
import com.tribalbattle.tribal_battle_api.intelligence.entity.IntelligenceWatchlistSettings;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligencePlayerRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceReportRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceVillageAnnotationRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceVillageRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceWatchlistEntryRepository;
import com.tribalbattle.tribal_battle_api.intelligence.repository.IntelligenceWatchlistSettingsRepository;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import com.tribalbattle.tribal_battle_api.simulationhistory.repository.SimulationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligenceService {

    private static final int DEFAULT_ALERT_THRESHOLD = 25;
    private static final int REPORT_LIMIT = 50;

    private final AuthService authService;
    private final SimulationHistoryRepository simulationHistoryRepository;
    private final IntelligenceIngestionService ingestionService;
    private final IntelligencePlayerRepository playerRepository;
    private final IntelligenceVillageRepository villageRepository;
    private final IntelligenceReportRepository reportRepository;
    private final IntelligenceWatchlistSettingsRepository watchlistSettingsRepository;
    private final IntelligenceWatchlistEntryRepository watchlistEntryRepository;
    private final IntelligenceVillageAnnotationRepository annotationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<IntelligenceReportResponse> listReports(String authorizationHeader) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        return reportRepository
                .findByUserIdOrderByObservedAtDesc(
                        userId,
                        PageRequest.of(0, REPORT_LIMIT)
                )
                .stream()
                .map(this::toReportResponse)
                .toList();
    }

    @Transactional
    public List<IntelligencePlayerResponse> listPlayers(String authorizationHeader) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        List<IntelligenceVillage> villages =
                villageRepository.findByUserIdOrderByLastSeenAtDesc(userId);

        Map<UUID, List<IntelligenceVillage>> villagesByPlayer =
                villages.stream().collect(
                        Collectors.groupingBy(IntelligenceVillage::getPlayerId)
                );

        return playerRepository
                .findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(player -> {
                    List<IntelligenceVillage> playerVillages =
                            villagesByPlayer.getOrDefault(player.getId(), List.of());

                    long reportCount =
                            playerVillages.stream()
                                    .mapToLong(village ->
                                            reportRepository.countByUserIdAndVillageId(
                                                    userId,
                                                    village.getId()
                                            )
                                    )
                                    .sum();

                    Instant lastSeen =
                            playerVillages.stream()
                                    .map(IntelligenceVillage::getLastSeenAt)
                                    .max(Comparator.naturalOrder())
                                    .orElse(player.getUpdatedAt());

                    return new IntelligencePlayerResponse(
                            player.getId(),
                            player.getName(),
                            playerVillages.size(),
                            reportCount,
                            lastSeen
                    );
                })
                .toList();
    }

    @Transactional
    public List<IntelligenceVillageResponse> listVillages(
            String authorizationHeader,
            UUID playerId,
            String search
    ) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        List<IntelligenceVillage> villages =
                playerId == null
                        ? villageRepository.findByUserIdOrderByLastSeenAtDesc(userId)
                        : villageRepository.findByUserIdAndPlayerIdOrderByLastSeenAtDesc(
                                userId,
                                playerId
                        );

        Map<UUID, IntelligencePlayer> players =
                playerRepository.findAllById(
                                villages.stream()
                                        .map(IntelligenceVillage::getPlayerId)
                                        .collect(Collectors.toSet())
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                IntelligencePlayer::getId,
                                Function.identity()
                        ));

        String normalizedSearch =
                search == null ? "" : search.trim().toLowerCase();

        return villages.stream()
                .filter(village -> {
                    if (normalizedSearch.isBlank()) {
                        return true;
                    }

                    IntelligencePlayer player = players.get(village.getPlayerId());
                    String coordinates =
                            village.getX() != null && village.getY() != null
                                    ? village.getX() + "|" + village.getY()
                                    : "";

                    return (
                            village.getName() + " " +
                            (player == null ? "" : player.getName()) + " " +
                            coordinates
                    ).toLowerCase().contains(normalizedSearch);
                })
                .map(village -> {
                    IntelligencePlayer player = players.get(village.getPlayerId());

                    return new IntelligenceVillageResponse(
                            village.getId(),
                            village.getPlayerId(),
                            village.getVillageKey(),
                            player == null ? "Unknown player" : player.getName(),
                            village.getName(),
                            village.getX(),
                            village.getY(),
                            reportRepository.countByUserIdAndVillageId(
                                    userId,
                                    village.getId()
                            ),
                            village.getLastSeenAt()
                    );
                })
                .toList();
    }

    @Transactional
    public IntelligenceWatchlistResponse getWatchlist(String authorizationHeader) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        return toWatchlistResponse(userId);
    }

    @Transactional
    public IntelligenceWatchlistResponse updateWatchlist(
            String authorizationHeader,
            UpdateIntelligenceWatchlistRequest request
    ) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        int threshold = Math.max(5, Math.min(200, request.alertThresholdPercent()));

        IntelligenceWatchlistSettings settings =
                watchlistSettingsRepository.findById(userId)
                        .orElseGet(() ->
                                IntelligenceWatchlistSettings.builder()
                                        .userId(userId)
                                        .build()
                        );

        settings.setAlertThresholdPercent(threshold);
        watchlistSettingsRepository.save(settings);

        Set<String> keys =
                request.watchedVillageKeys().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<IntelligenceVillage> villages =
                keys.isEmpty()
                        ? List.of()
                        : villageRepository.findByUserIdAndVillageKeyIn(userId, keys);

        watchlistEntryRepository.deleteByUserId(userId);

        List<IntelligenceWatchlistEntry> entries =
                villages.stream()
                        .map(village ->
                                IntelligenceWatchlistEntry.builder()
                                        .userId(userId)
                                        .villageId(village.getId())
                                        .build()
                        )
                        .toList();

        watchlistEntryRepository.saveAll(entries);

        return toWatchlistResponse(userId);
    }

    @Transactional
    public List<IntelligenceAnnotationResponse> listAnnotations(String authorizationHeader) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        Map<UUID, IntelligenceVillage> villages =
                villageRepository.findAllById(
                                annotationRepository.findByUserId(userId)
                                        .stream()
                                        .map(IntelligenceVillageAnnotation::getVillageId)
                                        .collect(Collectors.toSet())
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                IntelligenceVillage::getId,
                                Function.identity()
                        ));

        return annotationRepository.findByUserId(userId)
                .stream()
                .map(annotation -> {
                    IntelligenceVillage village = villages.get(annotation.getVillageId());
                    if (village == null) {
                        return null;
                    }
                    return toAnnotationResponse(annotation, village.getVillageKey());
                })
                .filter(value -> value != null)
                .toList();
    }

    @Transactional
    public IntelligenceAnnotationResponse updateAnnotation(
            String authorizationHeader,
            UpdateIntelligenceAnnotationRequest request
    ) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        IntelligenceVillage village =
                villageRepository
                        .findByUserIdAndVillageKey(userId, request.villageKey().trim())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Village is not available in server-side intelligence."
                                )
                        );

        IntelligenceVillageAnnotation annotation =
                annotationRepository
                        .findByUserIdAndVillageId(userId, village.getId())
                        .orElseGet(() ->
                                IntelligenceVillageAnnotation.builder()
                                        .userId(userId)
                                        .villageId(village.getId())
                                        .build()
                        );

        List<String> tags =
                request.tags().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .limit(20)
                        .toList();

        annotation.setTagsPayload(writeJson(tags));
        annotation.setNote(request.note().trim());

        IntelligenceVillageAnnotation saved = annotationRepository.save(annotation);
        return toAnnotationResponse(saved, village.getVillageKey());
    }

    @Transactional
    public void deleteAnnotation(
            String authorizationHeader,
            String villageKey
    ) {
        UUID userId = requireUser(authorizationHeader).getId();
        ensureBackfilled(userId);

        villageRepository
                .findByUserIdAndVillageKey(userId, villageKey.trim())
                .ifPresent(village ->
                        annotationRepository.deleteByUserIdAndVillageId(
                                userId,
                                village.getId()
                        )
                );
    }

    private IntelligenceWatchlistResponse toWatchlistResponse(UUID userId) {
        int threshold =
                watchlistSettingsRepository
                        .findById(userId)
                        .map(IntelligenceWatchlistSettings::getAlertThresholdPercent)
                        .orElse(DEFAULT_ALERT_THRESHOLD);

        List<IntelligenceWatchlistEntry> entries =
                watchlistEntryRepository.findByUserId(userId);

        Map<UUID, IntelligenceVillage> villages =
                villageRepository
                        .findAllById(
                                entries.stream()
                                        .map(IntelligenceWatchlistEntry::getVillageId)
                                        .collect(Collectors.toSet())
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                IntelligenceVillage::getId,
                                Function.identity()
                        ));

        List<String> keys =
                entries.stream()
                        .map(entry -> villages.get(entry.getVillageId()))
                        .filter(value -> value != null)
                        .map(IntelligenceVillage::getVillageKey)
                        .distinct()
                        .toList();

        return new IntelligenceWatchlistResponse(keys, threshold);
    }

    private IntelligenceReportResponse toReportResponse(IntelligenceReport report) {
        return new IntelligenceReportResponse(
                report.getSimulationHistoryId(),
                "server-intelligence",
                report.getSource(),
                readJson(report.getPayload()),
                readJson(report.getResultPayload()),
                readJson(report.getReportMetadataPayload()),
                false,
                report.getObservedAt(),
                true
        );
    }

    private IntelligenceAnnotationResponse toAnnotationResponse(
            IntelligenceVillageAnnotation annotation,
            String villageKey
    ) {
        return new IntelligenceAnnotationResponse(
                villageKey,
                readStringList(annotation.getTagsPayload()),
                annotation.getNote(),
                annotation.getUpdatedAt()
        );
    }

    private void ensureBackfilled(UUID userId) {
        long sourceCount =
                simulationHistoryRepository
                        .countByUserIdAndReportMetadataPayloadIsNotNull(userId);
        long normalizedCount = reportRepository.countByUserId(userId);

        if (normalizedCount >= sourceCount) {
            return;
        }

        List<SimulationHistory> history =
                simulationHistoryRepository
                        .findByUserIdAndReportMetadataPayloadIsNotNullOrderByCreatedAtAsc(userId);

        for (SimulationHistory item : history) {
            ingestionService.ingest(item);
        }
    }

    private AppUser requireUser(String authorizationHeader) {
        return authService.requireUser(
                authService.requireBearerToken(authorizationHeader)
        );
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored intelligence JSON is invalid.", exception);
        }
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        JsonNode node = readJson(value);
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize intelligence JSON.", exception);
        }
    }
}
