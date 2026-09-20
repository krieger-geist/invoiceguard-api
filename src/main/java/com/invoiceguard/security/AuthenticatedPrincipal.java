package com.invoiceguard.security;

import java.util.Set;
import java.util.UUID;

/**
 * The resolved identity of the caller for the current request, derived from
 * the JWT access token by {@link JwtAuthenticationFilter} and set as the
 * {@code principal} on the Spring Security {@code Authentication}.
 *
 * <p>Every module's service layer should read the caller's organisation from
 * here (via {@link TenantContext}) rather than trusting an {@code organizationId}
 * supplied in a request body/path — that is the core multi-tenant isolation
 * guarantee described in {@link com.invoiceguard.common.entity.OrganizationScopedEntity}.
 */
public record AuthenticatedPrincipal(
        UUID userId, String email, UUID organizationId, Role role, Set<Permission> permissions) {

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
