package com.invoiceguard.integration.apikey;

import com.invoiceguard.security.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.Set;

public record ApiKeyCreateRequest(@NotBlank String name, @NotEmpty Set<Permission> scopes, Instant expiresAt, Integer rateLimitPerMinute) {}
