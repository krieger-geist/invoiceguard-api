package com.invoiceguard.integration.webhook;

public enum WebhookDeliveryStatus {
    PENDING,
    DELIVERED,
    FAILED,
    RETRYING,
    EXHAUSTED
}
