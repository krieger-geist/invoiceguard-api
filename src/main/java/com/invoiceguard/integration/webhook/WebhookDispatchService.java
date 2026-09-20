package com.invoiceguard.integration.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispatches one event to every active {@link WebhookSubscription} in an
 * organisation that subscribes to it, HMAC-SHA256-signing each payload with
 * that subscription's own secret so the receiver can verify authenticity.
 *
 * <p>Retry policy: exponential backoff (2^attempt minutes, capped), up to
 * {@code invoiceguard.webhook.max-attempts}. {@link WebhookRetryScheduler}
 * picks up anything still {@code FAILED}/{@code RETRYING} whose
 * {@code nextRetryAt} has passed.
 */
@Service
public class WebhookDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatchService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryAttemptRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int maxAttempts;
    private final int timeoutMs;

    public WebhookDispatchService(
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookDeliveryAttemptRepository deliveryRepository,
            ObjectMapper objectMapper,
            @Value("${invoiceguard.webhook.max-attempts:6}") int maxAttempts,
            @Value("${invoiceguard.webhook.timeout-ms:5000}") int timeoutMs) {
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
        this.timeoutMs = timeoutMs;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
    }

    @Transactional
    public void dispatch(UUID organizationId, String eventType, Map<String, Object> payloadData) {
        for (WebhookSubscription subscription : subscriptionRepository.findByOrganizationIdAndActiveTrue(organizationId)) {
            if (!subscription.subscribesTo(eventType)) {
                continue;
            }

            UUID eventId = UUID.randomUUID();
            String payloadJson;
            try {
                payloadJson = objectMapper.writeValueAsString(Map.of(
                        "eventId", eventId.toString(), "eventType", eventType, "data", payloadData));
            } catch (Exception e) {
                log.error("Failed to serialize webhook payload for event {}", eventType, e);
                continue;
            }

            WebhookDeliveryAttempt attempt = new WebhookDeliveryAttempt();
            attempt.setOrganizationId(organizationId);
            attempt.setWebhookSubscriptionId(subscription.getId());
            attempt.setEventId(eventId);
            attempt.setEventType(eventType);
            attempt.setPayload(payloadJson);
            attempt.setStatus(WebhookDeliveryStatus.PENDING);
            attempt = deliveryRepository.save(attempt);

            attemptDelivery(subscription, attempt);
        }
    }

    @Transactional
    public void attemptDelivery(WebhookSubscription subscription, WebhookDeliveryAttempt attempt) {
        attempt.setAttemptCount(attempt.getAttemptCount() + 1);
        attempt.setLastAttemptAt(Instant.now());

        try {
            String signature = sign(attempt.getPayload(), subscription.getSigningSecret());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getUrl()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("X-Event-Id", attempt.getEventId().toString())
                    .header("X-Signature", "sha256=" + signature)
                    .POST(HttpRequest.BodyPublishers.ofString(attempt.getPayload(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            attempt.setHttpStatus(response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                attempt.setStatus(WebhookDeliveryStatus.DELIVERED);
                attempt.setNextRetryAt(null);
            } else {
                scheduleRetryOrExhaust(attempt);
            }
        } catch (Exception e) {
            log.warn("Webhook delivery failed for subscription {} (event {}): {}",
                    subscription.getId(), attempt.getEventType(), e.getMessage());
            scheduleRetryOrExhaust(attempt);
        }

        deliveryRepository.save(attempt);
    }

    private void scheduleRetryOrExhaust(WebhookDeliveryAttempt attempt) {
        if (attempt.getAttemptCount() >= maxAttempts) {
            attempt.setStatus(WebhookDeliveryStatus.EXHAUSTED);
            attempt.setNextRetryAt(null);
            return;
        }
        attempt.setStatus(WebhookDeliveryStatus.RETRYING);
        long backoffMinutes = (long) Math.pow(2, attempt.getAttemptCount());
        attempt.setNextRetryAt(Instant.now().plus(backoffMinutes, ChronoUnit.MINUTES));
    }

    private String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
