package com.invoiceguard.integration.webhook;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One delivery record per (subscription, event) pair. Retries update the
 * SAME row (incrementing {@code attemptCount}, bumping {@code nextRetryAt})
 * rather than creating a new one — that, plus a stable {@code eventId}
 * carried in every delivered payload's {@code X-Event-Id} header, is what
 * gives receivers idempotent delivery: they can safely dedupe on
 * {@code eventId} regardless of how many attempts it took to succeed.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_delivery_attempts")
public class WebhookDeliveryAttempt extends OrganizationScopedEntity {

    @Column(name = "webhook_subscription_id", nullable = false)
    private UUID webhookSubscriptionId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WebhookDeliveryStatus status = WebhookDeliveryStatus.PENDING;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
}
