package com.invoiceguard.auth.entity;

import com.invoiceguard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single refresh-token session, bound to one user acting within one
 * organisation. Only the SHA-256 hash of the token is stored — the raw
 * token is returned to the client exactly once, at issuance, and can never
 * be recovered from the database (mirrors how password hashing works).
 *
 * <p>Rotation policy: every successful {@code /auth/refresh} call revokes
 * this row and creates a new one, linked via {@code replacedByTokenId}. If a
 * revoked token is ever presented again, that is treated as a signal of
 * token theft and every refresh token for the user is revoked
 * (see {@code AuthService#refresh}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
