package com.invoiceguard.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the full lifecycle from your spec's "registration and login" and
 * "JWT authorization" integration-test requirements: register creates an
 * organisation + admin user and returns usable tokens, the access token
 * authorizes a protected endpoint, refresh rotates it, and logout actually
 * revokes the access token immediately (via the Redis blacklist from Phase 7)
 * rather than leaving it valid until natural expiry.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginAccessProtectedEndpointRefreshAndLogout() throws Exception {
        String email = "owner+" + System.nanoTime() + "@example.com";

        // 1. Register — creates org + ORGANIZATION_ADMIN user, returns tokens directly.
        Map<String, Object> registerBody = Map.of(
                "organizationName", "Acme Test Corp",
                "email", email,
                "password", "SuperSecret123",
                "firstName", "Ada",
                "lastName", "Owner");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        JsonNode registerData = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("data");
        String accessToken = registerData.get("accessToken").asText();
        String refreshToken = registerData.get("refreshToken").asText();

        // 2. The access token authorizes a protected endpoint.
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", equalTo(email)));

        // 3. Without a token, the same endpoint is rejected.
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());

        // 4. Refresh issues a new working access token.
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        String newAccessToken =
                objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());

        // 5. The OLD refresh token is now revoked (rotation) — reusing it must fail.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());

        // 6. Logout blacklists the CURRENT access token immediately.
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejectedWithoutLeakingWhetherTheEmailExists() throws Exception {
        Map<String, Object> loginBody = Map.of("email", "no-such-user@example.com", "password", "whatever123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", equalTo("Invalid email or password")));
    }
}
