package com.invoiceguard.integration.webhook;

import java.util.List;
import java.util.UUID;

public record WebhookSubscriptionResponse(UUID id, String url, List<String> eventTypes, boolean active) {}
