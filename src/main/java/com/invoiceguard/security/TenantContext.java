package com.invoiceguard.security;

import com.invoiceguard.exception.TenantAccessDeniedException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Convenience accessor for the current request's {@link AuthenticatedPrincipal}.
 *
 * <p>This is the single place every service in the codebase should go through
 * to find "which organisation is this request allowed to touch". Services
 * must call {@link #requireOrganizationId()} rather than reading
 * {@code organizationId} off an incoming DTO, so a caller can never widen
 * their own access by editing a request payload.
 */
@Component
public class TenantContext {

    public Optional<AuthenticatedPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public UUID requireOrganizationId() {
        return currentPrincipal()
                .map(AuthenticatedPrincipal::organizationId)
                .orElseThrow(() -> new TenantAccessDeniedException("No authenticated organisation context"));
    }

    public UUID requireUserId() {
        return currentPrincipal()
                .map(AuthenticatedPrincipal::userId)
                .orElseThrow(() -> new TenantAccessDeniedException("No authenticated user context"));
    }

    /** Throws if the given entity's organisation does not match the caller's tenant. */
    public void assertOwnedByCurrentOrganization(UUID entityOrganizationId, String entityName, UUID entityId) {
        UUID callerOrgId = requireOrganizationId();
        if (!callerOrgId.equals(entityOrganizationId)) {
            throw TenantAccessDeniedException.forEntity(entityName, entityId);
        }
    }
}
