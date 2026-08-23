package com.tribalbattle.tribal_battle_api.sharedSimulation;

import com.tribalbattle.tribal_battle_api.sharedsimulation.repository.SharedSimulationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SharedSimulationIntegrationTest {

    private static final String ENDPOINT =
            "/api/v1/shared-simulations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SharedSimulationRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateSharedSimulation() throws Exception {
        String requestBody = """
                {
                  "attacker": {
                    "axe": 1800,
                    "lightCavalry": 25
                  },
                  "defender": {
                    "spearman": 154,
                    "swordsman": 270
                  }
                }
                """;

        MvcResult result = mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.code").exists()
                )
                .andExpect(
                        jsonPath("$.code").isString()
                )
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result
                        .getResponse()
                        .getContentAsString()
        );

        String code = response
                .get("code")
                .asText();

        assertThat(code)
                .hasSize(8)
                .matches(
                        "[A-HJ-NP-Z2-9]{8}"
                );

        assertThat(
                repository.findByCode(code)
        ).isPresent();
    }

    @Test
    void shouldRetrieveSharedSimulation() throws Exception {
        String code = createSimulation();

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/"
                                        + code
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.attacker.axe"
                        ).value(1800)
                )
                .andExpect(
                        jsonPath(
                                "$.attacker.lightCavalry"
                        ).value(25)
                )
                .andExpect(
                        jsonPath(
                                "$.defender.spearman"
                        ).value(154)
                )
                .andExpect(
                        jsonPath(
                                "$.defender.swordsman"
                        ).value(270)
                );
    }

    @Test
    void shouldReturnNotFoundWhenSimulationDoesNotExist()
            throws Exception {

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/AAAAAAAA"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(404)
                )
                .andExpect(
                        jsonPath(
                                "$.error"
                        ).value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Simulation not found for code: AAAAAAAA"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.path"
                        ).value(
                                ENDPOINT
                                        + "/AAAAAAAA"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.timestamp"
                        ).exists()
                );
    }

    @Test
    void shouldReturnBadRequestWhenCodeIsInvalid()
            throws Exception {

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/ABC"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(400)
                )
                .andExpect(
                        jsonPath(
                                "$.error"
                        ).value(
                                "BAD_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Simulation code must contain 8 characters"
                        )
                );
    }

    @Test
    void shouldReturnBadRequestWhenAttackerIsMissing()
            throws Exception {

        String requestBody = """
                {
                  "defender": {
                    "spearman": 1000
                  }
                }
                """;

        mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(400)
                )
                .andExpect(
                        jsonPath(
                                "$.error"
                        ).value(
                                "BAD_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Attacker is required"
                        )
                );
    }

    @Test
    void shouldReturnBadRequestWhenDefenderIsMissing()
            throws Exception {

        String requestBody = """
                {
                  "attacker": {
                    "axe": 1000
                  }
                }
                """;

        mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(400)
                )
                .andExpect(
                        jsonPath(
                                "$.error"
                        ).value(
                                "BAD_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Defender is required"
                        )
                );
    }

    @Test
    void shouldReturnBadRequestWhenAttackerIsNotObject()
            throws Exception {

        String requestBody = """
                {
                  "attacker": 1000,
                  "defender": {
                    "spearman": 1000
                  }
                }
                """;

        mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Attacker must be an object"
                        )
                );
    }

    @Test
    void shouldReturnBadRequestWhenDefenderIsNotObject()
            throws Exception {

        String requestBody = """
                {
                  "attacker": {
                    "axe": 1000
                  },
                  "defender": "invalid"
                }
                """;

        mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Defender must be an object"
                        )
                );
    }

    @Test
    void shouldReturnBadRequestForMalformedJson()
            throws Exception {

        String requestBody = """
                {
                  "attacker":
                }
                """;

        mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath(
                                "$.status"
                        ).value(400)
                )
                .andExpect(
                        jsonPath(
                                "$.error"
                        ).value(
                                "BAD_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.message"
                        ).value(
                                "Invalid request body"
                        )
                );
    }

    @Test
    void shouldAcceptLowercaseShareCode()
            throws Exception {

        String code = createSimulation();

        mockMvc
                .perform(
                        get(
                                ENDPOINT
                                        + "/"
                                        + code.toLowerCase()
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.attacker.axe"
                        ).value(1800)
                );
    }

    private String createSimulation()
            throws Exception {

        String requestBody = """
                {
                  "attacker": {
                    "axe": 1800,
                    "lightCavalry": 25
                  },
                  "defender": {
                    "spearman": 154,
                    "swordsman": 270
                  }
                }
                """;

        MvcResult result = mockMvc
                .perform(
                        post(ENDPOINT)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        requestBody
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
                .get("code")
                .asText();
    }
}