package com.invoiceguard.integration.apikey;

import com.invoiceguard.security.AuthenticatedPrincipal;
import com.invoiceguard.security.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the {@code X-API-Key} header into an {@link AuthenticatedPrincipal}
 * for machine-to-machine callers, exactly parallel to how
 * {@code JwtAuthenticationFilter} resolves a Bearer token. Runs after the
 * JWT filter and only acts if the security context is still empty (a
 * request should never present both a valid JWT and an API key needing
 * separate handling).
 *
 * <p>An API key's "role" for authorization purposes is always
 * {@link Role#API_CLIENT}; its actual permissions come from the specific
 * scopes chosen at generation time (a strict subset of what {@code API_CLIENT}
 * could ever be granted — see {@code RolePermissions}), not the full
 * {@code API_CLIENT} default set.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawKey = request.getHeader(HEADER);
            if (rawKey != null && !rawKey.isBlank()) {
                apiKeyService.authenticate(rawKey).ifPresent(apiKey -> {
                    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                            apiKey.getId(), // synthetic "user" id — see ApiKey javadoc
                            "api-key:" + apiKey.getName(),
                            apiKey.getOrganizationId(),
                            Role.API_CLIENT,
                            ApiKeyService.parseScopes(apiKey.getScopes()));

                    List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + Role.API_CLIENT.name()));
                    principal.permissions()
                            .forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getAuthority())));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
