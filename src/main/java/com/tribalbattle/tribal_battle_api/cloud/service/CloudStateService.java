package com.tribalbattle.tribal_battle_api.cloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import com.tribalbattle.tribal_battle_api.cloud.dto.CloudStateResponse;
import com.tribalbattle.tribal_battle_api.cloud.dto.SaveCloudStateRequest;
import com.tribalbattle.tribal_battle_api.cloud.entity.UserCloudState;
import com.tribalbattle.tribal_battle_api.cloud.repository.UserCloudStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudStateService {

    private static final TypeReference<Map<String, String>> PAYLOAD_TYPE =
            new TypeReference<>() {
            };

    private final UserCloudStateRepository repository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${app.cloud.max-payload-bytes:5242880}")
    private long maxPayloadBytes;

    @Transactional(readOnly = true)
    public CloudStateResponse get(
            String authorizationHeader
    ) {
        AppUser user =
                authenticatedUser(
                        authorizationHeader
                );

        UserCloudState state =
                repository
                        .findById(
                                user.getId()
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "No cloud snapshot exists for this account."
                                )
                        );

        return toResponse(state);
    }

    @Transactional
    public CloudStateResponse save(
            String authorizationHeader,
            SaveCloudStateRequest request
    ) {
        AppUser user =
                authenticatedUser(
                        authorizationHeader
                );

        Map<String, String> payload =
                normalizePayload(
                        request.payload()
                );

        String serialized =
                serialize(payload);

        validateSize(serialized);

        UUID userId =
                user.getId();

        UserCloudState state =
                repository
                        .findForUpdate(userId)
                        .orElse(null);

        long currentRevision =
                state == null
                        ? 0
                        : state.getRevision();

        if (
                request.expectedRevision() !=
                        currentRevision
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cloud data changed on another device. Refresh before uploading."
            );
        }

        if (state == null) {
            state =
                    UserCloudState.builder()
                            .userId(userId)
                            .revision(1)
                            .payload(serialized)
                            .updatedAt(
                                    Instant.now()
                            )
                            .build();
        } else {
            state.setRevision(
                    currentRevision + 1
            );

            state.setPayload(
                    serialized
            );

            state.setUpdatedAt(
                    Instant.now()
            );
        }

        repository.saveAndFlush(state);

        return toResponse(state);
    }

    @Transactional
    public void delete(
            String authorizationHeader
    ) {
        AppUser user =
                authenticatedUser(
                        authorizationHeader
                );

        repository.deleteById(
                user.getId()
        );
    }

    private AppUser authenticatedUser(
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

    private Map<String, String> normalizePayload(
            Map<String, String> payload
    ) {
        Map<String, String> normalized =
                new LinkedHashMap<>();

        payload
                .entrySet()
                .stream()
                .sorted(
                        Map.Entry.comparingByKey()
                )
                .forEach(
                        entry -> {
                            String key =
                                    entry.getKey();

                            String value =
                                    entry.getValue();

                            if (
                                    key == null ||
                                    key.isBlank() ||
                                    value == null
                            ) {
                                return;
                            }

                            if (
                                    !key.startsWith(
                                            "tribal-battle-"
                                    ) ||
                                    key.startsWith(
                                            "tribal-battle-auth-"
                                    ) ||
                                    key.startsWith(
                                            "tribal-battle-cloud-sync-"
                                    )
                            ) {
                                throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Cloud payload contains a disallowed key."
                                );
                            }

                            if (key.length() > 180) {
                                throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Cloud payload key is too long."
                                );
                            }

                            normalized.put(
                                    key,
                                    value
                            );
                        }
                );

        return normalized;
    }

    private String serialize(
            Map<String, String> payload
    ) {
        try {
            return objectMapper
                    .writeValueAsString(
                            payload
                    );
        } catch (
                JsonProcessingException exception
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cloud payload could not be serialized.",
                    exception
            );
        }
    }

    private Map<String, String> deserialize(
            String payload
    ) {
        try {
            return objectMapper.readValue(
                    payload,
                    PAYLOAD_TYPE
            );
        } catch (
                JsonProcessingException exception
        ) {
            throw new IllegalStateException(
                    "Stored cloud payload is invalid.",
                    exception
            );
        }
    }

    private void validateSize(
            String payload
    ) {
        long bytes =
                payload
                        .getBytes(
                                StandardCharsets.UTF_8
                        )
                        .length;

        if (
                bytes >
                        Math.max(
                                1024,
                                maxPayloadBytes
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Cloud snapshot exceeds the configured size limit."
            );
        }
    }

    private CloudStateResponse toResponse(
            UserCloudState state
    ) {
        return new CloudStateResponse(
                state.getRevision(),
                state.getUpdatedAt(),
                deserialize(
                        state.getPayload()
                )
        );
    }
}
