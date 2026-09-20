package com.invoiceguard.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for every entity that belongs to exactly one organisation
 * (vendors, invoices, alerts, approvals, audit records, analytics inputs, ...).
 *
 * <p>{@code organizationId} is intentionally a plain column (not a JPA
 * relationship) so it can be indexed cheaply and used directly inside
 * {@link org.springframework.data.jpa.domain.Specification} predicates and
 * native queries without triggering a join. Every repository query against
 * a subclass of this entity MUST filter on {@code organizationId} — see
 * {@code com.invoiceguard.security.TenantContext} and the module-level
 * specifications for the enforcement pattern used throughout the codebase.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class OrganizationScopedEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;
}
