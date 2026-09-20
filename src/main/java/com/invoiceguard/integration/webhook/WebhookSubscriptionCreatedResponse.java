package com.invoiceguard.integration.webhook;

import java.util.List;
import java.util.UUID;

/** Returned only at creation — the signing secret is shown once so the receiving endpoint can be configured to verify it. */
public record WebhookSubscriptionCreatedResponse(UUID id, String url, List<String> eventTypes, boolean active, String signingSecret) {}
