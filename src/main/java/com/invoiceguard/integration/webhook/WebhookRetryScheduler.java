package com.invoiceguard.integration.webhook;

import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookRetryScheduler {

    private final WebhookDeliveryAttemptRepository deliveryRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDispatchService dispatchService;

    public WebhookRetryScheduler(
            WebhookDeliveryAttemptRepository deliveryRepository,
            WebhookSubscriptionRepository subscriptionRepository,
            WebhookDispatchService dispatchService) {
        this.deliveryRepository = deliveryRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${invoiceguard.webhook.retry-check-interval-ms:60000}")
    public void retryDueDeliveries() {
        List<WebhookDeliveryAttempt> due = deliveryRepository.findByStatusInAndNextRetryAtBefore(
                List.of(WebhookDeliveryStatus.RETRYING, WebhookDeliveryStatus.FAILED), Instant.now());

        for (WebhookDeliveryAttempt attempt : due) {
            subscriptionRepository
                    .findById(attempt.getWebhookSubscriptionId())
                    .filter(WebhookSubscription::isActive)
                    .ifPresent(subscription -> dispatchService.attemptDelivery(subscription, attempt));
        }
    }
}
