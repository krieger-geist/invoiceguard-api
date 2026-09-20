package com.invoiceguard.organization.entity;

import com.invoiceguard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tenant. Every organisation-scoped entity elsewhere in the system
 * (vendors, invoices, alerts, ...) carries this entity's id as its
 * {@code organizationId} column — see
 * {@link com.invoiceguard.common.entity.OrganizationScopedEntity}.
 *
 * <p>Deliberately does NOT extend {@code OrganizationScopedEntity} itself:
 * an organisation IS the tenant boundary, not a member of one.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(name = "plan", length = 50)
    private String plan;
}
