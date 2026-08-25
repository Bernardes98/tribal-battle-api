package com.tribalbattle.tribal_battle_api.simulationhistory.service;

import com.tribalbattle.tribal_battle_api.simulationhistory.dto.CreateSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistory;
import com.tribalbattle.tribal_battle_api.simulationhistory.exception.SimulationHistoryNotFoundException;
import com.tribalbattle.tribal_battle_api.simulationhistory.repository.SimulationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    public SimulationHistoryResponse create(
            CreateSimulationHistoryRequest request
    ) {
        String clientId =
                normalizeClientId(
                        request.clientId()
                );

        SimulationHistory history =
                SimulationHistory.builder()
                        .clientId(clientId)
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
        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        return repository
                .findTop50ByClientIdOrderByCreatedAtDesc(
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
        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        SimulationHistory history =
                repository
                        .findByIdAndClientId(
                                id,
                                normalizedClientId
                        )
                        .orElseThrow(
                                () ->
                                        new SimulationHistoryNotFoundException(
                                                id
                                        )
                        );

        return toResponse(history);
    }

    public void delete(
            UUID id,
            String clientId
    ) {
        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        SimulationHistory history =
                repository
                        .findByIdAndClientId(
                                id,
                                normalizedClientId
                        )
                        .orElseThrow(
                                () ->
                                        new SimulationHistoryNotFoundException(
                                                id
                                        )
                        );

        repository.delete(history);
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
