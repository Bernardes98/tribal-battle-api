package com.tribalbattle.tribal_battle_api.cloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import com.tribalbattle.tribal_battle_api.cloud.dto.CloudStateResponse;
import com.tribalbattle.tribal_battle_api.cloud.dto.CloudStateVersionResponse;
import com.tribalbattle.tribal_battle_api.cloud.dto.RestoreCloudStateRequest;
import com.tribalbattle.tribal_battle_api.cloud.dto.SaveCloudStateRequest;
import com.tribalbattle.tribal_battle_api.cloud.entity.UserCloudState;
import com.tribalbattle.tribal_battle_api.cloud.entity.UserCloudStateVersion;
import com.tribalbattle.tribal_battle_api.cloud.repository.UserCloudStateRepository;
import com.tribalbattle.tribal_battle_api.cloud.repository.UserCloudStateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudStateService {

    private static final TypeReference<Map<String, String>> PAYLOAD_TYPE =
            new TypeReference<>() {
            };

    private final UserCloudStateRepository repository;
    private final UserCloudStateVersionRepository versionRepository;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${app.cloud.max-payload-bytes:5242880}")
    private long maxPayloadBytes;

    @Value("${app.cloud.version-retention:10}")
    private int versionRetention;

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

    @Transactional(readOnly = true)
    public List<CloudStateVersionResponse> versions(
            String authorizationHeader
    ) {
        AppUser user =
                authenticatedUser(
                        authorizationHeader
                );

        UUID userId =
                user.getId();

        List<CloudStateVersionResponse> versions =
                new ArrayList<>();

        repository
                .findById(userId)
                .ifPresent(
                        state ->
                                versions.add(
                                        new CloudStateVersionResponse(
                                                state.getRevision(),
                                                state.getUpdatedAt(),
                                                true
                                        )
                                )
                );

        versionRepository
                .findByUserIdOrderByRevisionDesc(userId)
                .forEach(
                        version ->
                                versions.add(
                                        new CloudStateVersionResponse(
                                                version.getRevision(),
                                                version.getSnapshotAt(),
                                                false
                                        )
                                )
                );

        versions.sort(
                Comparator.comparingLong(
                                CloudStateVersionResponse::revision
                        )
                        .reversed()
        );

        return versions;
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

        requireExpectedRevision(
                request.expectedRevision(),
                currentRevision
        );

        if (state != null) {
            archive(state);
        }

        long nextRevision =
                nextRevision(
                        userId,
                        state
                );

        if (state == null) {
            state =
                    UserCloudState.builder()
                            .userId(userId)
                            .revision(nextRevision)
                            .payload(serialized)
                            .updatedAt(
                                    Instant.now()
                            )
                            .build();
        } else {
            state.setRevision(
                    nextRevision
            );

            state.setPayload(
                    serialized
            );

            state.setUpdatedAt(
                    Instant.now()
            );
        }

        repository.saveAndFlush(state);

        pruneVersions(userId);

        return toResponse(state);
    }

    @Transactional
    public CloudStateResponse restore(
            String authorizationHeader,
            long revision,
            RestoreCloudStateRequest request
    ) {
        AppUser user =
                authenticatedUser(
                        authorizationHeader
                );

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

        requireExpectedRevision(
                request.expectedRevision(),
                currentRevision
        );

        UserCloudStateVersion source =
                versionRepository
                        .findByUserIdAndRevision(
                                userId,
                                revision
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Cloud backup revision was not found."
                                )
                        );

        if (state != null) {
            archive(state);
        }

        long nextRevision =
                nextRevision(
                        userId,
                        state
                );

        if (state == null) {
            state =
                    UserCloudState.builder()
                            .userId(userId)
                            .revision(nextRevision)
                            .payload(source.getPayload())
                            .updatedAt(
                                    Instant.now()
                            )
                            .build();
        } else {
            state.setRevision(
                    nextRevision
            );

            state.setPayload(
                    source.getPayload()
            );

            state.setUpdatedAt(
                    Instant.now()
            );
        }

        repository.saveAndFlush(state);

        pruneVersions(userId);

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

        UUID userId =
                user.getId();

        UserCloudState state =
                repository
                        .findForUpdate(userId)
                        .orElse(null);

        if (state == null) {
            return;
        }

        archive(state);

        repository.delete(state);
        repository.flush();

        pruneVersions(userId);
    }

    private void requireExpectedRevision(
            long expectedRevision,
            long currentRevision
    ) {
        if (
                expectedRevision !=
                        currentRevision
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cloud data changed on another device. Refresh before changing cloud data."
            );
        }
    }

    private long nextRevision(
            UUID userId,
            UserCloudState current
    ) {
        long archivedRevision =
                versionRepository
                        .findMaxRevision(userId)
                        .orElse(0L);

        long currentRevision =
                current == null
                        ? 0
                        : current.getRevision();

        return Math.max(
                currentRevision,
                archivedRevision
        ) + 1;
    }

    private void archive(
            UserCloudState state
    ) {
        if (
                versionRepository
                        .existsByUserIdAndRevision(
                                state.getUserId(),
                                state.getRevision()
                        )
        ) {
            return;
        }

        versionRepository.saveAndFlush(
                UserCloudStateVersion.builder()
                        .id(UUID.randomUUID())
                        .userId(
                                state.getUserId()
                        )
                        .revision(
                                state.getRevision()
                        )
                        .payload(
                                state.getPayload()
                        )
                        .snapshotAt(
                                state.getUpdatedAt()
                        )
                        .archivedAt(
                                Instant.now()
                        )
                        .build()
        );
    }

    private void pruneVersions(
            UUID userId
    ) {
        int retention =
                Math.max(
                        1,
                        Math.min(
                                versionRetention,
                                50
                        )
                );

        List<UserCloudStateVersion> versions =
                versionRepository
                        .findByUserIdOrderByRevisionDesc(
                                userId
                        );

        if (
                versions.size() <=
                        retention
        ) {
            return;
        }

        versionRepository.deleteAllInBatch(
                versions.subList(
                        retention,
                        versions.size()
                )
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
