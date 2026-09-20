package com.invoiceguard.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the spec's "duplicate invoice submission" and idempotency-key
 * integration-test requirements together: the SAME Idempotency-Key with the
 * SAME body must return the original invoice rather than creating a second
 * one, and the same key with a DIFFERENT body must be rejected outright.
 */
class InvoiceIdempotencyIntegrationTest extends AbstractIntegrationTest {

    private String registerAndGetToken() throws Exception {
        long nonce = System.nanoTime();
        Map<String, Object> body = Map.of(
                "organizationName", "Idempotency Test Org " + nonce,
                "email", "idem+" + nonce + "@example.com",
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

    private String createVendor(String token) throws Exception {
        Map<String, Object> body = Map.of("legalName", "Test Vendor Inc", "country", "US");
        MvcResult result = mockMvc.perform(post("/api/v1/vendors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();
    }

    private Map<String, Object> invoiceBody(String vendorId, String invoiceNumber, String total) {
        return Map.of(
                "invoiceNumber", invoiceNumber,
                "vendorId", vendorId,
                "invoiceDate", "2026-06-01",
                "currency", "USD",
                "subtotal", total,
                "taxAmount", "0",
                "totalAmount", total);
    }

    @Test
    void sameIdempotencyKeyAndBodyReturnsTheOriginalInvoice() throws Exception {
        String token = registerAndGetToken();
        String vendorId = createVendor(token);
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> body = invoiceBody(vendorId, "INV-9001", "100.00");

        MvcResult first = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        String firstInvoiceId =
                objectMapper.readTree(first.getResponse().getContentAsString()).get("data").get("id").asText();

        // Same key, same body: replayed — same invoice ID, HTTP 200 (not 201, nothing new was created).
        MvcResult second = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        String secondInvoiceId =
                objectMapper.readTree(second.getResponse().getContentAsString()).get("data").get("id").asText();

        org.assertj.core.api.Assertions.assertThat(secondInvoiceId).isEqualTo(firstInvoiceId);
    }

    @Test
    void sameIdempotencyKeyWithDifferentBodyIsRejected() throws Exception {
        String token = registerAndGetToken();
        String vendorId = createVendor(token);
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceBody(vendorId, "INV-9002", "100.00"))))
                .andExpect(status().isCreated());

        // Same key, different amount — must be rejected, not silently create a second invoice.
        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceBody(vendorId, "INV-9002", "999.00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", org.hamcrest.Matchers.equalTo("IDEMPOTENCY_KEY_REUSED")));
    }

    @Test
    void invoiceCreationWithoutIdempotencyKeyStillWorks() throws Exception {
        String token = registerAndGetToken();
        String vendorId = createVendor(token);

        mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invoiceBody(vendorId, "INV-9003", "250.00"))))
                .andExpect(status().isCreated());
    }
}
