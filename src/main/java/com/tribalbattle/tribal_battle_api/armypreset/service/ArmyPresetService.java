package com.tribalbattle.tribal_battle_api.armypreset.service;

import com.tribalbattle.tribal_battle_api.armypreset.dto.ArmyPresetResponse;
import com.tribalbattle.tribal_battle_api.armypreset.dto.CreateArmyPresetRequest;
import com.tribalbattle.tribal_battle_api.armypreset.entity.ArmyPreset;
import com.tribalbattle.tribal_battle_api.armypreset.exception.ArmyPresetNotFoundException;
import com.tribalbattle.tribal_battle_api.armypreset.repository.ArmyPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArmyPresetService {

    private final ArmyPresetRepository repository;

    private final ObjectMapper objectMapper;

    public ArmyPresetResponse create(
            CreateArmyPresetRequest request
    ) {
        String clientId =
                normalizeClientId(
                        request.clientId()
                );

        String name =
                normalizeName(
                        request.name()
                );

        ArmyPreset preset =
                ArmyPreset.builder()
                        .clientId(clientId)
                        .name(name)
                        .type(request.type())
                        .armyPayload(
                                writeJson(
                                        request.army()
                                )
                        )
                        .contextPayload(
                                request.context() == null
                                        ? null
                                        : writeJson(
                                                request.context()
                                        )
                        )
                        .build();

        return toResponse(
                repository.save(preset)
        );
    }

    public List<ArmyPresetResponse> list(
            String clientId
    ) {
        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        return repository
                .findTop100ByClientIdOrderByTypeAscUpdatedAtDesc(
                        normalizedClientId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(
            UUID id,
            String clientId
    ) {
        String normalizedClientId =
                normalizeClientId(
                        clientId
                );

        ArmyPreset preset =
                repository
                        .findByIdAndClientId(
                                id,
                                normalizedClientId
                        )
                        .orElseThrow(
                                () ->
                                        new ArmyPresetNotFoundException(
                                                id
                                        )
                        );

        repository.delete(preset);
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
                    "Invalid army preset client id",
                    exception
            );
        }
    }

    private String normalizeName(
            String name
    ) {
        String normalized =
                name.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Army preset name is required"
            );
        }

        if (normalized.length() > 80) {
            throw new IllegalArgumentException(
                    "Army preset name must contain at most 80 characters"
            );
        }

        return normalized;
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
                    "Invalid army preset payload",
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
                    "Could not read stored army preset",
                    exception
            );
        }
    }

    private ArmyPresetResponse toResponse(
            ArmyPreset preset
    ) {
        return new ArmyPresetResponse(
                preset.getId(),
                preset.getClientId(),
                preset.getName(),
                preset.getType(),
                readJson(
                        preset.getArmyPayload()
                ),
                preset.getContextPayload() == null
                        ? null
                        : readJson(
                                preset.getContextPayload()
                        ),
                preset.getCreatedAt(),
                preset.getUpdatedAt()
        );
    }
}
