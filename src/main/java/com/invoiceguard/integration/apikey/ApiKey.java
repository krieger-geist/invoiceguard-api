package com.invoiceguard.integration.apikey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A machine-to-machine credential. Only {@link #hashedKey} is ever
 * persisted — the raw key is returned to the caller exactly once, at
 * generation, exactly like refresh tokens (see {@code OpaqueTokenService},
 * reused here for both generation and hashing).
 *
 * <p>{@code scopes} reuses {@link com.invoiceguard.security.Permission}
 * directly rather than inventing a parallel scope enum — the spec's example
 * scopes ({@code invoice:read}, {@code invoice:write}, {@code vendor:read},
 * {@code vendor:write}, {@code analytics:read}) are already exactly a
 * subset of that enum's authority strings.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "api_keys")
public class ApiKey extends OrganizationScopedEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Non-secret, safe to display — e.g. "igk_ab12cd34" — so a user can tell keys apart without re-exposing the secret. */
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @JsonIgnore
    @Column(name = "hashed_key", nullable = false, unique = true, length = 128)
    private String hashedKey;

    /** Comma-separated {@link com.invoiceguard.security.Permission} authority strings. */
    @Column(name = "scopes", nullable = false, length = 500)
    private String scopes;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    public boolean isActive() {
        return !revoked && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
