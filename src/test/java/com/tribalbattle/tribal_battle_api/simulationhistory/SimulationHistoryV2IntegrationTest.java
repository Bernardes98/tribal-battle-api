package com.tribalbattle.tribal_battle_api.simulationhistory;

import com.tribalbattle.tribal_battle_api.simulationhistory.repository.SimulationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationHistoryV2IntegrationTest {

    private static final String ENDPOINT =
            "/api/v1/simulation-history";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationHistoryRepository repository;

    private String clientId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        clientId =
                UUID.randomUUID()
                        .toString();
    }

    @Test
    void shouldPaginateFilterAndSearchHistory()
            throws Exception {
        for (int index = 0; index < 12; index++) {
            createHistory(
                    index % 3 == 0
                            ? "SPY_REPORT"
                            : index % 3 == 1
                            ? "BATTLE_REPORT"
                            : "MANUAL",
                    index % 2 == 0
                            ? "FelipeG98"
                            : "SolRain",
                    index % 2 == 0
                            ? "North Keep"
                            : "South Keep"
            );
        }

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/search"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .param(
                                        "page",
                                        "0"
                                )
                                .param(
                                        "size",
                                        "5"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(12)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(3)
                );

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/search"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .param(
                                        "source",
                                        "SPY_REPORT"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(4)
                );

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/search"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .param(
                                        "player",
                                        "felipe"
                                )
                                .param(
                                        "village",
                                        "north"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(6)
                );

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/search"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .param(
                                        "search",
                                        "South Keep"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(6)
                );
    }

    @Test
    void shouldFavoriteAndFilterFavorites()
            throws Exception {
        String id =
                createHistory(
                        "BATTLE_REPORT",
                        "FelipeG98",
                        "Capital"
                );

        mockMvc
                .perform(
                        patch(
                                ENDPOINT
                                        + "/"
                                        + id
                                        + "/favorite"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "favorite": true
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.favorite")
                                .value(true)
                );

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/search"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .param(
                                        "favorite",
                                        "true"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(id)
                );
    }

    @Test
    void shouldBulkDeleteOnlyRequestedHistory()
            throws Exception {
        List<String> ids =
                new ArrayList<>();

        ids.add(
                createHistory(
                        "MANUAL",
                        "Player One",
                        "Village One"
                )
        );
        ids.add(
                createHistory(
                        "SPY_REPORT",
                        "Player Two",
                        "Village Two"
                )
        );
        ids.add(
                createHistory(
                        "BATTLE_REPORT",
                        "Player Three",
                        "Village Three"
                )
        );

        String request =
                """
                {
                  "ids": ["%s", "%s"]
                }
                """.formatted(
                        ids.get(0),
                        ids.get(1)
                );

        mockMvc
                .perform(
                        post(
                                ENDPOINT
                                        + "/bulk-delete"
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.deletedCount")
                                .value(2)
                );

        assertThat(
                repository.count()
        ).isEqualTo(1);
    }

    private String createHistory(
            String source,
            String player,
            String village
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
                    }
                  },
                  "result": {
                    "winner": "ATTACKER"
                  },
                  "reportMetadata": {
                    "attacker": {
                      "playerName": "%s",
                      "villageName": "%s",
                      "coordinates": {
                        "x": 499,
                        "y": 511
                      }
                    },
                    "defender": {
                      "playerName": "Target Player",
                      "villageName": "Target Village",
                      "coordinates": {
                        "x": 501,
                        "y": 516
                      }
                    }
                  }
                }
                """.formatted(
                        clientId,
                        source,
                        player,
                        village
                );

        MvcResult result =
                mockMvc
                        .perform(
                                post(ENDPOINT)
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(request)
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        JsonNode response =
                objectMapper.readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                );

        return response
                .get("id")
                .asText();
    }
}
