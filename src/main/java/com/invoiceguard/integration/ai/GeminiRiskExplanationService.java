package com.invoiceguard.integration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.risk.entity.RiskFinding;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Calls Google's Gemini API to turn a risk finding list into a friendlier,
 * more fluent plain-language summary than the deterministic template can
 * produce — used only for {@code RiskAssessment.aiSummary}, never for the
 * guaranteed {@code explanation} field (see {@code RuleBasedRiskExplanationService}
 * and {@code RiskEngine}). Registered as a Spring bean only when
 * {@code invoiceguard.ai.enabled=true}; with AI disabled (the default) this
 * class is never instantiated and the app has zero dependency on it.
 *
 * <p>Per the spec: AI never decides anything here — it only rephrases
 * findings the deterministic engine already computed. Every call logs
 * provider, model, and latency; invoice content itself is not logged.
 */
@Service
@ConditionalOnProperty(prefix = "invoiceguard.ai", name = "enabled", havingValue = "true")
public class GeminiRiskExplanationService implements RiskExplanationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiRiskExplanationService.class);
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiRiskExplanationService(AiProperties properties, ObjectMapper objectMapper) {
        if (!"gemini".equalsIgnoreCase(properties.provider())) {
            throw new IllegalStateException(
                    "invoiceguard.ai.enabled=true but invoiceguard.ai.provider is '" + properties.provider()
                            + "' — only 'gemini' is currently supported");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "invoiceguard.ai.enabled=true with provider=gemini requires GEMINI_API_KEY to be set");
        }
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.timeoutMs()))
                .build();
    }

    @Override
    public String explain(Invoice invoice, List<RiskFinding> findings) {
        long start = System.currentTimeMillis();
        try {
            String prompt = buildPrompt(invoice, findings);
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

            String model = properties.model() != null && !properties.model().isBlank() ? properties.model() : "gemini-2.0-flash";
            URI uri = URI.create(API_BASE + model + ":generateContent?key=" + properties.apiKey());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(properties.timeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - start;

            if (response.statusCode() != 200) {
                log.warn("Gemini call failed (provider=gemini, model={}, status={}, latencyMs={})", model, response.statusCode(), latencyMs);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            log.info("Gemini risk explanation generated (provider=gemini, model={}, latencyMs={})", model, latencyMs);
            return text;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.warn("Gemini call errored (provider=gemini, latencyMs={}): {}", latencyMs, e.getMessage());
            return null;
        }
    }

    private String buildPrompt(Invoice invoice, List<RiskFinding> findings) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are assisting an accounts-payable reviewer. Summarize the following invoice risk ")
                .append("findings in 2-3 plain-language sentences for a busy finance manager. Be factual and ")
                .append("concise. Do not recommend approving or rejecting the invoice — only summarize the findings.\n\n");
        sb.append("Invoice: ").append(invoice.getInvoiceNumber())
                .append(", amount: ").append(invoice.getTotalAmount()).append(" ").append(invoice.getCurrency())
                .append("\n\nFindings:\n");
        for (RiskFinding finding : findings) {
            sb.append("- [").append(finding.getSeverity()).append("] ").append(finding.getTitle())
                    .append(": ").append(finding.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
