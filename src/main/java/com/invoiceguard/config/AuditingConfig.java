package com.invoiceguard.config;

import java.util.Optional;

import com.invoiceguard.security.AuthenticatedPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Supplies the "current user" for {@code @CreatedBy}/{@code @LastModifiedBy}
 * auditing fields on {@link com.invoiceguard.common.entity.BaseEntity}.
 *
 * <p>Reads the authenticated principal's username from the Spring Security
 * context once the auth module (Phase 2) populates it; falls back to
 * {@code "system"} for unauthenticated contexts such as startup seeding.
 */
@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .filter(Authentication::isAuthenticated)
                    .map(Authentication::getPrincipal)
                    .filter(AuthenticatedPrincipal.class::isInstance)
                    .map(AuthenticatedPrincipal.class::cast)
                    .map(AuthenticatedPrincipal::email)
                    .or(() -> Optional.of("system"));
        };
    }
}
