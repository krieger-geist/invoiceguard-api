package com.invoiceguard.organization.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import com.invoiceguard.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links a {@link com.invoiceguard.user.entity.User} to an
 * {@link Organization} with a single {@link Role}. This is the join entity
 * that makes "which organisations can this user act as, and with what role"
 * a queryable fact rather than an assumption baked into the user record.
 *
 * <p>{@code organizationId} (inherited from {@link OrganizationScopedEntity})
 * is the tenant this membership belongs to; {@code userId} is the member.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "organization_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_id"}))
public class OrganizationMember extends OrganizationScopedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrganizationMemberStatus status = OrganizationMemberStatus.ACTIVE;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "joined_at")
    private Instant joinedAt;
}
