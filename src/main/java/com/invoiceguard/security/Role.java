package com.invoiceguard.security;

/**
 * System-defined roles a user can hold within a single organisation
 * (via {@code OrganizationMember.role}). Roles are fixed, not
 * organisation-configurable — see {@link RolePermissions} for what each
 * role grants.
 */
public enum Role {
    SUPER_ADMIN,
    ORGANIZATION_ADMIN,
    FINANCE_MANAGER,
    ANALYST,
    REVIEWER,
    AUDITOR,
    API_CLIENT
}
