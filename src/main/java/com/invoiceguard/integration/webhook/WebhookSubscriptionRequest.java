package com.invoiceguard.integration.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import org.hibernate.validator.constraints.URL;

public record WebhookSubscriptionRequest(@NotBlank @URL String url, @NotEmpty Set<String> eventTypes, boolean active) {}
