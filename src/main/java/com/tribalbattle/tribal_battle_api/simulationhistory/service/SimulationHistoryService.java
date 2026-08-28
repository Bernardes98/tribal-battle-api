package com.tribalbattle.tribal_battle_api.simulationhistory.service;

import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.BulkDeleteSimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.ClaimSimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.CreateSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryPageResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistorySource;
import com.tribalbattle.tribal_battle_api.simulationhistory.exception.SimulationHistoryNotFoundException;
import com.tribalbattle.tribal_battle_api.simulationhistory.repository.SimulationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimulationHistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final int MAX_SEARCH_LENGTH = 120;

    private final SimulationHistoryRepository repository;

    private final ObjectMapper objectMapper;

    private final AuthService authService;

    /*
     * Compatibility overload used by older tests/internal callers.
     * It preserves the original guest/browser behavior.
     */
    public SimulationHistoryResponse create(
            CreateSimulationHistoryRequest request
    ) {
        return create(
                request,
                null
        );
    }

    @Transactional
    public SimulationHistoryResponse create(
            CreateSimulationHistoryRequest request,
            String authorizationHeader
    ) {
        String clientId =
                normalizeClientId(
                        request.clientId()
                );

        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        JsonNode reportMetadata =
                request.reportMetadata();

        SimulationHistory history =
                SimulationHistory.builder()
                        .clientId(clientId)
                        .userId(
                                user == null
                                        ? null
                                        : user.getId()
                        )
                        .source(request.source())
                        .payload(
                                writeJson(
                                        request.payload()
                                )
                        )
                        .resultPayload(
                                request.result() == null
                                        ? null
                                        : writeJson(
                                                request.result()
                                        )
                        )
                        .reportMetadataPayload(
                                reportMetadata == null
                                        ? null
                                        : writeJson(
                                                reportMetadata
                                        )
                        )
                        .attackerPlayerName(
                                readMetadataText(
                                        reportMetadata,
                                        "attacker",
                                        "playerName",
                                        "player"
                                )
                        )
                        .defenderPlayerName(
                                readMetadataText(
                                        reportMetadata,
                                        "defender",
                                        "playerName",
                                        "player"
                                )
                        )
                        .attackerVillageName(
                                readMetadataText(
                                        reportMetadata,
                                        "attacker",
                                        "villageName",
                                        "village"
                                )
                        )
                        .defenderVillageName(
                                readMetadataText(
                                        reportMetadata,
                                        "defender",
                                        "villageName",
                                        "village"
                                )
                        )
                        .build();

        return toResponse(
                repository.save(history)
        );
    }

    public List<SimulationHistoryResponse> list(
            String clientId
    ) {
        return list(
                clientId,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<SimulationHistoryResponse> list(
            String clientId,
            String authorizationHeader
    ) {
        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        if (user != null) {
            return repository
                    .findTop50ByUserIdOrderByCreatedAtDesc(
                            user.getId()
                    )
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        return repository
                .findTop50ByClientIdAndUserIdIsNullOrderByCreatedAtDesc(
                        normalizedClientId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SimulationHistoryPageResponse search(
            String clientId,
            String authorizationHeader,
            int page,
            int size,
            SimulationHistorySource source,
            String player,
            String village,
            Instant from,
            Instant to,
            Boolean favorite,
            String search,
            String sort,
            String direction
    ) {
        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        String normalizedClientId =
                user == null
                        ? normalizeClientId(clientId)
                        : null;

        int normalizedPage =
                Math.max(
                        page,
                        0
                );

        int normalizedSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        MAX_PAGE_SIZE
                );

        Pageable pageable =
                PageRequest.of(
                        normalizedPage,
                        normalizedSize,
                        historySort(
                                sort,
                                direction
                        )
                );

        Specification<SimulationHistory> specification =
                historySpecification(
                        user,
                        normalizedClientId,
                        source,
                        player,
                        village,
                        from,
                        to,
                        favorite,
                        search
                );

        Page<SimulationHistory> result =
                repository.findAll(
                        specification,
                        pageable
                );

        return new SimulationHistoryPageResponse(
                result
                        .getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    public SimulationHistoryResponse findById(
            UUID id,
            String clientId
    ) {
        return findById(
                id,
                clientId,
                null
        );
    }

    @Transactional(readOnly = true)
    public SimulationHistoryResponse findById(
            UUID id,
            String clientId,
            String authorizationHeader
    ) {
        return toResponse(
                findOwnedHistory(
                        id,
                        clientId,
                        authorizationHeader
                )
        );
    }

    public void delete(
            UUID id,
            String clientId
    ) {
        delete(
                id,
                clientId,
                null
        );
    }

    @Transactional
    public void delete(
            UUID id,
            String clientId,
            String authorizationHeader
    ) {
        repository.delete(
                findOwnedHistory(
                        id,
                        clientId,
                        authorizationHeader
                )
        );
    }

    @Transactional
    public BulkDeleteSimulationHistoryResponse bulkDelete(
            List<UUID> ids,
            String clientId,
            String authorizationHeader
    ) {
        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        String normalizedClientId =
                user == null
                        ? normalizeClientId(clientId)
                        : null;

        Specification<SimulationHistory> specification =
                (root, query, builder) -> {
                    List<jakarta.persistence.criteria.Predicate> predicates =
                            new ArrayList<>();

                    predicates.add(
                            ownerPredicate(
                                    root,
                                    builder,
                                    user,
                                    normalizedClientId
                            )
                    );

                    predicates.add(
                            root
                                    .get("id")
                                    .in(ids)
                    );

                    return builder.and(
                            predicates.toArray(
                                    jakarta.persistence.criteria.Predicate[]::new
                            )
                    );
                };

        List<SimulationHistory> histories =
                repository.findAll(
                        specification
                );

        repository.deleteAllInBatch(
                histories
        );

        return new BulkDeleteSimulationHistoryResponse(
                histories.size()
        );
    }

    @Transactional
    public SimulationHistoryResponse updateFavorite(
            UUID id,
            boolean favorite,
            String clientId,
            String authorizationHeader
    ) {
        SimulationHistory history =
                findOwnedHistory(
                        id,
                        clientId,
                        authorizationHeader
                );

        history.setFavorite(
                favorite
        );

        return toResponse(
                repository.save(history)
        );
    }

    @Transactional
    public ClaimSimulationHistoryResponse claim(
            String clientId,
            String authorizationHeader
    ) {
        AppUser user =
                requireAuthenticatedUser(
                        authorizationHeader
                );

        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        int claimedCount =
                repository
                        .claimAnonymousHistory(
                                normalizedClientId,
                                user.getId()
                        );

        return new ClaimSimulationHistoryResponse(
                claimedCount
        );
    }

    private Specification<SimulationHistory> historySpecification(
            AppUser user,
            String clientId,
            SimulationHistorySource source,
            String player,
            String village,
            Instant from,
            Instant to,
            Boolean favorite,
            String search
    ) {
        String playerTerm =
                normalizeSearchTerm(
                        player
                );

        String villageTerm =
                normalizeSearchTerm(
                        village
                );

        String searchTerm =
                normalizeSearchTerm(
                        search
                );

        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            predicates.add(
                    ownerPredicate(
                            root,
                            builder,
                            user,
                            clientId
                    )
            );

            if (source != null) {
                predicates.add(
                        builder.equal(
                                root.get("source"),
                                source
                        )
                );
            }

            if (from != null) {
                predicates.add(
                        builder.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                from
                        )
                );
            }

            if (to != null) {
                predicates.add(
                        builder.lessThanOrEqualTo(
                                root.get("createdAt"),
                                to
                        )
                );
            }

            if (favorite != null) {
                predicates.add(
                        builder.equal(
                                root.get("favorite"),
                                favorite
                        )
                );
            }

            if (playerTerm != null) {
                predicates.add(
                        builder.or(
                                containsIgnoreCase(
                                        root.get("attackerPlayerName"),
                                        builder,
                                        playerTerm
                                ),
                                containsIgnoreCase(
                                        root.get("defenderPlayerName"),
                                        builder,
                                        playerTerm
                                ),
                                containsIgnoreCase(
                                        root.get("reportMetadataPayload"),
                                        builder,
                                        playerTerm
                                )
                        )
                );
            }

            if (villageTerm != null) {
                predicates.add(
                        builder.or(
                                containsIgnoreCase(
                                        root.get("attackerVillageName"),
                                        builder,
                                        villageTerm
                                ),
                                containsIgnoreCase(
                                        root.get("defenderVillageName"),
                                        builder,
                                        villageTerm
                                ),
                                containsIgnoreCase(
                                        root.get("reportMetadataPayload"),
                                        builder,
                                        villageTerm
                                )
                        )
                );
            }

            if (searchTerm != null) {
                List<jakarta.persistence.criteria.Predicate> searchPredicates =
                        new ArrayList<>();

                searchPredicates.add(
                        containsIgnoreCase(
                                root.get("attackerPlayerName"),
                                builder,
                                searchTerm
                        )
                );
                searchPredicates.add(
                        containsIgnoreCase(
                                root.get("defenderPlayerName"),
                                builder,
                                searchTerm
                        )
                );
                searchPredicates.add(
                        containsIgnoreCase(
                                root.get("attackerVillageName"),
                                builder,
                                searchTerm
                        )
                );
                searchPredicates.add(
                        containsIgnoreCase(
                                root.get("defenderVillageName"),
                                builder,
                                searchTerm
                        )
                );
                searchPredicates.add(
                        containsIgnoreCase(
                                root.get("reportMetadataPayload"),
                                builder,
                                searchTerm
                        )
                );

                SimulationHistorySource searchedSource =
                        sourceFromSearch(
                                searchTerm
                        );

                if (searchedSource != null) {
                    searchPredicates.add(
                            builder.equal(
                                    root.get("source"),
                                    searchedSource
                            )
                    );
                }

                predicates.add(
                        builder.or(
                                searchPredicates.toArray(
                                        jakarta.persistence.criteria.Predicate[]::new
                                )
                        )
                );
            }

            return builder.and(
                    predicates.toArray(
                            jakarta.persistence.criteria.Predicate[]::new
                    )
            );
        };
    }

    private jakarta.persistence.criteria.Predicate ownerPredicate(
            jakarta.persistence.criteria.Root<SimulationHistory> root,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            AppUser user,
            String clientId
    ) {
        if (user != null) {
            return builder.equal(
                    root.get("userId"),
                    user.getId()
            );
        }

        return builder.and(
                builder.isNull(
                        root.get("userId")
                ),
                builder.equal(
                        root.get("clientId"),
                        clientId
                )
        );
    }

    private jakarta.persistence.criteria.Predicate containsIgnoreCase(
            jakarta.persistence.criteria.Path<String> path,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            String value
    ) {
        return builder.like(
                builder.lower(path),
                "%" + escapeLike(value) + "%",
                '\\'
        );
    }

    private Sort historySort(
            String sort,
            String direction
    ) {
        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        String normalizedSort =
                sort == null
                        ? "createdat"
                        : sort
                                .trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );

        List<Sort.Order> orders =
                switch (normalizedSort) {
                    case "source" -> List.of(
                            new Sort.Order(
                                    sortDirection,
                                    "source"
                            ),
                            Sort.Order.desc(
                                    "createdAt"
                            )
                    );
                    case "player" -> List.of(
                            new Sort.Order(
                                    sortDirection,
                                    "attackerPlayerName"
                            ).nullsLast(),
                            new Sort.Order(
                                    sortDirection,
                                    "defenderPlayerName"
                            ).nullsLast(),
                            Sort.Order.desc(
                                    "createdAt"
                            )
                    );
                    case "village" -> List.of(
                            new Sort.Order(
                                    sortDirection,
                                    "attackerVillageName"
                            ).nullsLast(),
                            new Sort.Order(
                                    sortDirection,
                                    "defenderVillageName"
                            ).nullsLast(),
                            Sort.Order.desc(
                                    "createdAt"
                            )
                    );
                    case "favorite" -> List.of(
                            new Sort.Order(
                                    sortDirection,
                                    "favorite"
                            ),
                            Sort.Order.desc(
                                    "createdAt"
                            )
                    );
                    default -> List.of(
                            new Sort.Order(
                                    sortDirection,
                                    "createdAt"
                            )
                    );
                };

        return Sort.by(orders)
                .and(
                        Sort.by(
                                Sort.Order.desc(
                                        "id"
                                )
                        )
                );
    }

    private SimulationHistory findOwnedHistory(
            UUID id,
            String clientId,
            String authorizationHeader
    ) {
        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        if (user != null) {
            return repository
                    .findByIdAndUserId(
                            id,
                            user.getId()
                    )
                    .orElseThrow(
                            () ->
                                    new SimulationHistoryNotFoundException(
                                            id
                                    )
                    );
        }

        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        return repository
                .findByIdAndClientIdAndUserIdIsNull(
                        id,
                        normalizedClientId
                )
                .orElseThrow(
                        () ->
                                new SimulationHistoryNotFoundException(
                                        id
                                )
                );
    }

    private AppUser optionalAuthenticatedUser(
            String authorizationHeader
    ) {
        if (
                authorizationHeader == null ||
                authorizationHeader.isBlank()
        ) {
            return null;
        }

        return requireAuthenticatedUser(
                authorizationHeader
        );
    }

    private AppUser requireAuthenticatedUser(
            String authorizationHeader
    ) {
        String token =
                authService
                        .requireBearerToken(
                                authorizationHeader
                        );

        return authService.requireUser(
                token
        );
    }

    private String normalizeClientId(
            String clientId
    ) {
        try {
            return UUID
                    .fromString(
                            clientId.trim()
                    )
                    .toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid simulation history client id",
                    exception
            );
        }
    }

    private String normalizeSearchTerm(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (normalized.isBlank()) {
            return null;
        }

        return normalized.length() > MAX_SEARCH_LENGTH
                ? normalized.substring(
                        0,
                        MAX_SEARCH_LENGTH
                )
                : normalized;
    }

    private String escapeLike(
            String value
    ) {
        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "%",
                        "\\%"
                )
                .replace(
                        "_",
                        "\\_"
                );
    }

    private SimulationHistorySource sourceFromSearch(
            String value
    ) {
        String normalized =
                value
                        .replace(
                                "_",
                                " "
                        )
                        .replace(
                                "-",
                                " "
                        )
                        .trim();

        if (
                "manual".contains(normalized) ||
                normalized.contains("manual")
        ) {
            return SimulationHistorySource.MANUAL;
        }

        if (
                normalized.contains("spy") ||
                normalized.contains("scout")
        ) {
            return SimulationHistorySource.SPY_REPORT;
        }

        if (
                normalized.contains("battle") ||
                normalized.contains("report")
        ) {
            return SimulationHistorySource.BATTLE_REPORT;
        }

        return null;
    }

    private String readMetadataText(
            JsonNode metadata,
            String party,
            String primaryField,
            String legacyField
    ) {
        if (metadata == null) {
            return null;
        }

        JsonNode partyNode =
                metadata.get(party);

        if (partyNode == null) {
            return null;
        }

        String value =
                textValue(
                        partyNode.get(
                                primaryField
                        )
                );

        if (value == null) {
            value =
                    textValue(
                            partyNode.get(
                                    legacyField
                            )
                    );
        }

        if (value == null) {
            return null;
        }

        return value.length() > 255
                ? value.substring(
                        0,
                        255
                )
                : value;
    }

    private String textValue(
            JsonNode node
    ) {
        if (
                node == null ||
                !node.isTextual()
        ) {
            return null;
        }

        String value =
                node
                        .asText()
                        .trim();

        return value.isBlank()
                ? null
                : value;
    }

    private String writeJson(
            JsonNode value
    ) {
        try {
            return objectMapper
                    .writeValueAsString(
                            value
                    );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Invalid simulation history payload",
                    exception
            );
        }
    }

    private JsonNode readJson(
            String value
    ) {
        try {
            return objectMapper
                    .readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not read stored simulation history",
                    exception
            );
        }
    }

    private SimulationHistoryResponse toResponse(
            SimulationHistory history
    ) {
        return new SimulationHistoryResponse(
                history.getId(),
                history.getClientId(),
                history.getSource(),
                readJson(
                        history.getPayload()
                ),
                history.getResultPayload() == null
                        ? null
                        : readJson(
                                history.getResultPayload()
                        ),
                history.getReportMetadataPayload() == null
                        ? null
                        : readJson(
                                history.getReportMetadataPayload()
                        ),
                history.isFavorite(),
                history.getCreatedAt()
        );
    }
}
