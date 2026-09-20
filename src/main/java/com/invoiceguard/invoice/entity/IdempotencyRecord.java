package com.invoiceguard.invoice.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Backs the {@code Idempotency-Key} header on invoice creation. A row here
 * means "this organisation already ran this exact request" — see
 * {@code IdempotencyService} for the hash-compare/replay logic.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "idempotency_key"}))
public class IdempotencyRecord extends OrganizationScopedEntity {

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Column(name = "response_reference")
    private UUID responseReference;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
