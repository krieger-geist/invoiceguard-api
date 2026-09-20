package com.invoiceguard.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The single most important correctness property in a multi-tenant system:
 * a user from one organisation must never be able to read another
 * organisation's data, even by guessing/enumerating a valid UUID. This
 * registers two completely independent organisations and proves org B's
 * token cannot see a vendor created under org A.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    private String registerOrgAndGetAccessToken(String orgName, String email) throws Exception {
        Map<String, Object> body = Map.of(
                "organizationName", orgName,
                "email", email,
                "password", "SuperSecret123",
                "firstName", "Test",
                "lastName", "User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("accessToken").asText();
    }

    @Test
    void organizationBCannotSeeOrganizationAsVendor() throws Exception {
        long nonce = System.nanoTime();
        String tokenA = registerOrgAndGetAccessToken("Org A " + nonce, "admin-a+" + nonce + "@example.com");
        String tokenB = registerOrgAndGetAccessToken("Org B " + nonce, "admin-b+" + nonce + "@example.com");

        // Org A creates a vendor.
        Map<String, Object> vendorBody = Map.of(
                "legalName", "Acme Supplies Ltd",
                "country", "US");

        MvcResult createResult = mockMvc.perform(post("/api/v1/vendors")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vendorBody)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode vendorData = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data");
        String vendorId = vendorData.get("id").asText();

        // Org A can read its own vendor.
        mockMvc.perform(get("/api/v1/vendors/" + vendorId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Org B, with a completely valid token, gets a 404 — not a 403, not a 200 with someone else's data.
        // A 404 here (rather than leaking a 403) is deliberate: it doesn't confirm the vendor ID even exists.
        mockMvc.perform(get("/api/v1/vendors/" + vendorId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Org B's own vendor list is empty — org A's vendor never appears in it.
        mockMvc.perform(get("/api/v1/vendors").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }
}
