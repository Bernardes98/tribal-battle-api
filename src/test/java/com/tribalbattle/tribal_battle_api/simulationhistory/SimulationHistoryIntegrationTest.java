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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationHistoryIntegrationTest {

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
    void shouldCreateAndListHistory()
            throws Exception {

        createHistory();

        mockMvc
                .perform(
                        get(ENDPOINT)
                                .param(
                                        "clientId",
                                        clientId
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$[0].source")
                                .value("MANUAL")
                )
                .andExpect(
                        jsonPath("$[0].payload.attacker.axe")
                                .value(1000)
                )
                .andExpect(
                        jsonPath("$[0].reportMetadata.attacker.player")
                                .value("FelipeG98")
                )
                .andExpect(
                        jsonPath("$[0].reportMetadata.defender.coordinates.x")
                                .value(501)
                );
    }

    @Test
    void shouldDeleteHistory()
            throws Exception {

        String id =
                createHistory();

        mockMvc
                .perform(
                        delete(
                                ENDPOINT
                                        + "/"
                                        + id
                        )
                                .param(
                                        "clientId",
                                        clientId
                                )
                )
                .andExpect(
                        status().isNoContent()
                );

        assertThat(
                repository.count()
        ).isZero();
    }

    @Test
    void shouldNotReturnHistoryFromAnotherClient()
            throws Exception {

        String id =
                createHistory();

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/"
                                        + id
                        )
                                .param(
                                        "clientId",
                                        UUID.randomUUID()
                                                .toString()
                                )
                )
                .andExpect(
                        status().isNotFound()
                );
    }

    private String createHistory()
            throws Exception {

        String request =
                """
                {
                  "clientId": "%s",
                  "source": "MANUAL",
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
                      "player": "FelipeG98",
                      "village": "[001] F",
                      "coordinates": {
                        "x": 499,
                        "y": 511
                      }
                    },
                    "defender": {
                      "player": "SolRain",
                      "village": "Salvihyard",
                      "coordinates": {
                        "x": 501,
                        "y": 516
                      }
                    }
                  }
                }
                """.formatted(
                        clientId
                );

        MvcResult result =
                mockMvc
                        .perform(
                                post(ENDPOINT)
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                request
                                        )
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
