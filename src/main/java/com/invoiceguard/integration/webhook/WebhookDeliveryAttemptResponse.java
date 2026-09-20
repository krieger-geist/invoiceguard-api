package com.invoiceguard.integration.webhook;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryAttemptResponse(
        UUID id, UUID eventId, String eventType, WebhookDeliveryStatus status, Integer httpStatus, int attemptCount,
        Instant lastAttemptAt, Instant nextRetryAt) {}
