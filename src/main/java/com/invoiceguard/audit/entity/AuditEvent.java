package com.invoiceguard.audit.entity;

import com.invoiceguard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An immutable audit trail row. No controller or service exposes an update
 * or delete for this entity — per the spec, normal users must never be able
 * to alter audit records, and the simplest way to guarantee that is to never
 * write the code path that would allow it.
 *
 * <p>Deliberately extends {@link BaseEntity} rather than
 * {@code OrganizationScopedEntity}: most audit rows do carry an
 * organisation (set via the nullable {@code organizationId} column below),
 * but a few security-relevant events — most notably a failed login attempt
 * for an email that doesn't resolve to any organisation yet — happen before
 * any tenant context exists. Forcing a NOT NULL organisation column would
 * mean either fabricating a fake tenant for those rows or silently dropping
 * them; neither is acceptable for a security audit trail, so the column is
 * nullable and {@code GET /audit-logs} simply scopes its query to the
 * caller's organisation like every other list endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_events")
public class AuditEvent extends BaseEntity {

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AuditResult result;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
