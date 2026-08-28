package com.tribalbattle.tribal_battle_api.simulationhistory.service;

import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.ClaimSimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.CreateSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import com.tribalbattle.tribal_battle_api.simulationhistory.exception.SimulationHistoryNotFoundException;
import com.tribalbattle.tribal_battle_api.simulationhistory.repository.SimulationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimulationHistoryService {

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
                                request.reportMetadata() == null
                                        ? null
                                        : writeJson(
                                                request.reportMetadata()
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
        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        SimulationHistory history;

        if (user != null) {
            history =
                    repository
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
        } else {
            String normalizedClientId =
                    normalizeClientId(
                            clientId
                    );

            history =
                    repository
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

        return toResponse(history);
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
        AppUser user =
                optionalAuthenticatedUser(
                        authorizationHeader
                );

        SimulationHistory history;

        if (user != null) {
            history =
                    repository
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
        } else {
            String normalizedClientId =
                    normalizeClientId(
                            clientId
                    );

            history =
                    repository
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

        repository.delete(history);
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
                history.getCreatedAt()
        );
    }
}
