package com.invoiceguard.integration.webhook;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttempt, UUID> {

    List<WebhookDeliveryAttempt> findByStatusInAndNextRetryAtBefore(List<WebhookDeliveryStatus> statuses, Instant threshold);

    List<WebhookDeliveryAttempt> findByWebhookSubscriptionIdOrderByCreatedAtDesc(UUID webhookSubscriptionId);
}
