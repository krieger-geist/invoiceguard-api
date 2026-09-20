package com.invoiceguard.integration.apikey;

import java.util.UUID;

public record ApiKeyGeneratedEvent(UUID apiKeyId, UUID organizationId, String name) {}
