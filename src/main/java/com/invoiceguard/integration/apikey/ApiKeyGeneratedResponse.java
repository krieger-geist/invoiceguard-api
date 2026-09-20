package com.invoiceguard.integration.apikey;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Returned only once, at creation — the raw key can never be retrieved again after this response. */
public record ApiKeyGeneratedResponse(UUID id, String name, String rawKey, String keyPrefix, List<String> scopes, Instant expiresAt) {}
