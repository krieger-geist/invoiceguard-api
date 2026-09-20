package com.invoiceguard.integration.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id, String name, String keyPrefix, List<String> scopes, boolean revoked, Instant expiresAt, Instant lastUsedAt, Instant createdAt) {}
