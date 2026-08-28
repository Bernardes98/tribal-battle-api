package com.tribalbattle.tribal_battle_api.intelligence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IntelligenceServerSideIntegrationTest {

    private static final String INTELLIGENCE_ENDPOINT =
            "/api/v1/intelligence";

    private static final String HISTORY_ENDPOINT =
            "/api/v1/simulation-history";

    private static final String AUTH_ENDPOINT =
            "/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldNormalizeAuthenticatedReportsAndExposeOwnedQueries()
            throws Exception {
        String token = register("normalizer");
        String clientId = UUID.randomUUID().toString();

        String historyId = createHistory(
                clientId,
                token,
                "SPY_REPORT",
                "Enemy Player",
                "Enemy Village",
                501,
                516
        );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/reports")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(historyId)
                )
                .andExpect(
                        jsonPath("$[0].source")
                                .value("SPY_REPORT")
                )
                .andExpect(
                        jsonPath("$[0].reportMetadata.defender.playerName")
                                .value("Enemy Player")
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/players")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Enemy Player")
                )
                .andExpect(
                        jsonPath("$[0].villageCount")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].reportCount")
                                .value(1)
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/villages")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].villageKey")
                                .value("enemy player::501|516")
                )
                .andExpect(
                        jsonPath("$[0].playerName")
                                .value("Enemy Player")
                )
                .andExpect(
                        jsonPath("$[0].villageName")
                                .value("Enemy Village")
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/reports")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldBackfillClaimedAnonymousHistory()
            throws Exception {
        String clientId = UUID.randomUUID().toString();

        String historyId = createHistory(
                clientId,
                null,
                "BATTLE_REPORT",
                "Backfill Player",
                "Backfill Village",
                477,
                488
        );

        String token = register("backfill");

        mockMvc.perform(
                        post(HISTORY_ENDPOINT + "/claim")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "clientId": "%s"
                                        }
                                        """.formatted(clientId)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.claimedCount")
                                .value(1)
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/reports")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(historyId)
                )
                .andExpect(
                        jsonPath("$[0].reportMetadata.defender.playerName")
                                .value("Backfill Player")
                );
    }

    @Test
    void shouldPersistWatchlistAndAnnotationsPerUser()
            throws Exception {
        String ownerToken = register("owner");
        String otherToken = register("other");
        String clientId = UUID.randomUUID().toString();
        String villageKey = "target player::512|513";

        createHistory(
                clientId,
                ownerToken,
                "SPY_REPORT",
                "Target Player",
                "Target Village",
                512,
                513
        );

        mockMvc.perform(
                        put(INTELLIGENCE_ENDPOINT + "/watchlist")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(ownerToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "watchedVillageKeys": ["%s"],
                                          "alertThresholdPercent": 40
                                        }
                                        """.formatted(villageKey)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.watchedVillageKeys[0]")
                                .value(villageKey)
                )
                .andExpect(
                        jsonPath("$.alertThresholdPercent")
                                .value(40)
                );

        mockMvc.perform(
                        put(INTELLIGENCE_ENDPOINT + "/annotations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(ownerToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "villageKey": "%s",
                                          "tags": ["Target", "Priority"],
                                          "note": "Watch noble timing"
                                        }
                                        """.formatted(villageKey)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.villageKey")
                                .value(villageKey)
                )
                .andExpect(
                        jsonPath("$.tags.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.note")
                                .value("Watch noble timing")
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/annotations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(ownerToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].note")
                                .value("Watch noble timing")
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/watchlist")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(otherToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.watchedVillageKeys.length()")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.alertThresholdPercent")
                                .value(25)
                );

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/annotations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(otherToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );

        mockMvc.perform(
                        delete(INTELLIGENCE_ENDPOINT + "/annotations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(ownerToken)
                                )
                                .param(
                                        "villageKey",
                                        villageKey
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get(INTELLIGENCE_ENDPOINT + "/annotations")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(ownerToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    private String register(String suffix)
            throws Exception {
        String email =
                "v57-" + suffix + "-" + UUID.randomUUID() + "@example.com";

        MvcResult result =
                mockMvc.perform(
                                post(AUTH_ENDPOINT + "/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "displayName": "V57 %s",
                                                  "email": "%s",
                                                  "password": "V57SecurePassword123!"
                                                }
                                                """.formatted(
                                                        suffix,
                                                        email
                                                )
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andReturn();

        JsonNode response =
                objectMapper.readTree(
                        result.getResponse().getContentAsString()
                );

        return response.get("token").asText();
    }

    private String createHistory(
            String clientId,
            String token,
            String source,
            String defenderPlayer,
            String defenderVillage,
            int x,
            int y
    ) throws Exception {
        String request =
                """
                {
                  "clientId": "%s",
                  "source": "%s",
                  "payload": {
                    "attacker": {
                      "axe": 1000
                    },
                    "defender": {
                      "swordsman": 500
                    },
                    "defenderModifiers": {
                      "wallLevel": 15
                    }
                  },
                  "result": {
                    "winner": "ATTACKER"
                  },
                  "reportMetadata": {
                    "attacker": {
                      "playerName": "Scout",
                      "villageName": "Scout Village",
                      "coordinates": {
                        "x": 500,
                        "y": 500
                      }
                    },
                    "defender": {
                      "playerName": "%s",
                      "villageName": "%s",
                      "coordinates": {
                        "x": %d,
                        "y": %d
                      }
                    }
                  }
                }
                """.formatted(
                        clientId,
                        source,
                        defenderPlayer,
                        defenderVillage,
                        x,
                        y
                );

        var builder =
                post(HISTORY_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request);

        if (token != null) {
            builder.header(
                    HttpHeaders.AUTHORIZATION,
                    bearer(token)
            );
        }

        MvcResult result =
                mockMvc.perform(builder)
                        .andExpect(status().isCreated())
                        .andReturn();

        JsonNode response =
                objectMapper.readTree(
                        result.getResponse().getContentAsString()
                );

        return response.get("id").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
