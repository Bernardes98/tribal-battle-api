package com.tribalbattle.tribal_battle_api.sharedsimulation.service;

import com.tribalbattle.tribal_battle_api.exception.InvalidSimulationException;
import com.tribalbattle.tribal_battle_api.exception.SimulationNotFoundException;
import com.tribalbattle.tribal_battle_api.sharedsimulation.entity.SharedSimulation;
import com.tribalbattle.tribal_battle_api.sharedsimulation.repository.SharedSimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class SharedSimulationService {

    private static final String CODE_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int CODE_LENGTH = 8;

    private static final int MAX_CODE_ATTEMPTS = 20;

    private final SharedSimulationRepository repository;

    private final ObjectMapper objectMapper;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public String create(JsonNode simulation) {
        validateSimulation(simulation);

        String code =
                generateUniqueCode();

        String payload;

        try {
            payload =
                    objectMapper.writeValueAsString(
                            simulation
                    );
        } catch (JacksonException exception) {
            throw new InvalidSimulationException(
                    "Invalid simulation payload",
                    exception
            );
        }

        SharedSimulation entity =
                SharedSimulation.builder()
                        .code(code)
                        .payload(payload)
                        .build();

        repository.save(entity);

        return code;
    }

    public JsonNode findByCode(String code) {
        String normalizedCode =
                normalizeCode(code);

        SharedSimulation simulation =
                repository
                        .findByCode(normalizedCode)
                        .orElseThrow(() ->
                                new SimulationNotFoundException(
                                        normalizedCode
                                )
                        );

        try {
            return objectMapper.readTree(
                    simulation.getPayload()
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not read stored simulation",
                    exception
            );
        }
    }

    private void validateSimulation(
            JsonNode simulation
    ) {
        if (
                simulation == null
                        || simulation.isNull()
                        || !simulation.isObject()
        ) {
            throw new InvalidSimulationException(
                    "Simulation payload is required"
            );
        }

        if (!simulation.has("attacker")) {
            throw new InvalidSimulationException(
                    "Attacker is required"
            );
        }

        if (!simulation.has("defender")) {
            throw new InvalidSimulationException(
                    "Defender is required"
            );
        }

        if (
                !simulation
                        .get("attacker")
                        .isObject()
        ) {
            throw new InvalidSimulationException(
                    "Attacker must be an object"
            );
        }

        if (
                !simulation
                        .get("defender")
                        .isObject()
        ) {
            throw new InvalidSimulationException(
                    "Defender must be an object"
            );
        }
    }

    private String normalizeCode(
            String code
    ) {
        if (
                code == null
                        || code.isBlank()
        ) {
            throw new InvalidSimulationException(
                    "Simulation code is required"
            );
        }

        String normalized =
                code
                        .trim()
                        .toUpperCase();

        if (
                normalized.length()
                        != CODE_LENGTH
        ) {
            throw new InvalidSimulationException(
                    "Simulation code must contain "
                            + CODE_LENGTH
                            + " characters"
            );
        }

        return normalized;
    }

    private String generateUniqueCode() {
        for (
                int attempt = 0;
                attempt < MAX_CODE_ATTEMPTS;
                attempt++
        ) {
            String code =
                    generateCode();

            if (
                    !repository.existsByCode(
                            code
                    )
            ) {
                return code;
            }
        }

        throw new IllegalStateException(
                "Could not generate unique simulation code"
        );
    }

    private String generateCode() {
        StringBuilder code =
                new StringBuilder(
                        CODE_LENGTH
                );

        for (
                int index = 0;
                index < CODE_LENGTH;
                index++
        ) {
            int characterIndex =
                    secureRandom.nextInt(
                            CODE_CHARACTERS.length()
                    );

            code.append(
                    CODE_CHARACTERS.charAt(
                            characterIndex
                    )
            );
        }

        return code.toString();
    }
}