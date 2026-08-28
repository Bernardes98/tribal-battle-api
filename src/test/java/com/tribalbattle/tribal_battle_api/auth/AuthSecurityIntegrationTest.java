package com.tribalbattle.tribal_battle_api.auth;

import com.tribalbattle.tribal_battle_api.auth.entity.AppSession;
import com.tribalbattle.tribal_battle_api.auth.repository.AppSessionRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.AppUserRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    private static final String ENDPOINT =
            "/api/v1/auth";

    private static final String EMAIL =
            "v54@example.com";

    private static final String CURRENT_PASSWORD =
            "CurrentPass123";

    private static final String NEW_PASSWORD =
            "NewSecurePass456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppSessionRepository sessionRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireCurrentPasswordWhenChangingPassword()
            throws Exception {

        String token = register();

        mockMvc
                .perform(
                        post(ENDPOINT + "/password/change")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "currentPassword": "WrongPassword123",
                                          "newPassword": "%s"
                                        }
                                        """.formatted(
                                                NEW_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("CURRENT_PASSWORD_INVALID")
                );

        mockMvc
                .perform(
                        get(ENDPOINT + "/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void shouldChangePasswordAndRevokeEveryOtherSession()
            throws Exception {

        String currentToken = register();
        String otherToken = login(CURRENT_PASSWORD);

        mockMvc
                .perform(
                        post(ENDPOINT + "/password/change")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(currentToken)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "currentPassword": "%s",
                                          "newPassword": "%s"
                                        }
                                        """.formatted(
                                                CURRENT_PASSWORD,
                                                NEW_PASSWORD
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.revokedSessions")
                                .value(1)
                );

        mockMvc
                .perform(
                        get(ENDPOINT + "/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(currentToken)
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc
                .perform(
                        get(ENDPOINT + "/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(otherToken)
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("SESSION_REVOKED")
                );

        loginExpectingUnauthorized(
                CURRENT_PASSWORD,
                "INVALID_CREDENTIALS"
        );

        assertThat(
                login(NEW_PASSWORD)
        ).isNotBlank();
    }

    @Test
    void shouldSignOutEverySessionIncludingCurrent()
            throws Exception {

        String currentToken = register();
        String otherToken = login(CURRENT_PASSWORD);

        mockMvc
                .perform(
                        post(ENDPOINT + "/sessions/revoke-all")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(currentToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.revokedCount")
                                .value(2)
                );

        assertSessionRevoked(currentToken);
        assertSessionRevoked(otherToken);
    }

    @Test
    void shouldReturnSpecificMessageForExpiredSession()
            throws Exception {

        String token = register();

        AppSession session =
                sessionRepository
                        .findAll()
                        .get(0);

        session.setExpiresAt(
                Instant.now()
                        .minusSeconds(60)
        );

        sessionRepository.save(session);

        mockMvc
                .perform(
                        get(ENDPOINT + "/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("SESSION_EXPIRED")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Your session has expired. Please sign in again.")
                );
    }

    private String register()
            throws Exception {

        MvcResult result =
                mockMvc
                        .perform(
                                post(ENDPOINT + "/register")
                                        .header(
                                                HttpHeaders.USER_AGENT,
                                                "V54 Integration Browser"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "displayName": "V54 User",
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """.formatted(
                                                        EMAIL,
                                                        CURRENT_PASSWORD
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        return readToken(result);
    }

    private String login(
            String password
    ) throws Exception {
        MvcResult result =
                mockMvc
                        .perform(
                                post(ENDPOINT + "/login")
                                        .header(
                                                HttpHeaders.USER_AGENT,
                                                "V54 Second Browser"
                                        )
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """.formatted(
                                                        EMAIL,
                                                        password
                                                )
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return readToken(result);
    }

    private void loginExpectingUnauthorized(
            String password,
            String code
    ) throws Exception {
        mockMvc
                .perform(
                        post(ENDPOINT + "/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(
                                                EMAIL,
                                                password
                                        )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(code)
                );
    }

    private void assertSessionRevoked(
            String token
    ) throws Exception {
        mockMvc
                .perform(
                        get(ENDPOINT + "/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("SESSION_REVOKED")
                );
    }

    private String readToken(
            MvcResult result
    ) throws Exception {
        JsonNode response =
                objectMapper.readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                );

        return response
                .get("token")
                .asText();
    }

    private String bearer(
            String token
    ) {
        return "Bearer " + token;
    }
}
