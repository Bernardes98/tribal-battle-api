package com.tribalbattle.tribal_battle_api.web;

import com.tribalbattle.tribal_battle_api.exception.ApiErrorResponse;
import com.tribalbattle.tribal_battle_api.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiRootIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ApiRootController())
                .build();
    }

    @Test
    void shouldExposeHealthyApiRoot() throws Exception {
        mockMvc
                .perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tribal Battle API"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldAcceptHeadProbeAtApiRoot() throws Exception {
        mockMvc
                .perform(head("/"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundForUnknownResource() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/resource-that-does-not-exist"
        );
        request.setRequestURI("/resource-that-does-not-exist");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleNoResourceFound(
                        null,
                        request
                );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("NOT_FOUND", response.getBody().error());
        assertEquals("Resource not found", response.getBody().message());
        assertEquals(
                "/resource-that-does-not-exist",
                response.getBody().path()
        );
    }
}
