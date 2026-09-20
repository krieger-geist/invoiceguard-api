package com.invoiceguard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts and validates the {@code Authorization: Bearer <jwt>} header on
 * every request. On success, populates the Spring Security context with an
 * {@link AuthenticatedPrincipal} (as the {@code Authentication.principal})
 * whose granted authorities are the caller's role and resolved permissions —
 * this is what makes {@code @PreAuthorize("hasAuthority('invoice:write')")}
 * and {@code hasRole(...)} work downstream.
 *
 * <p>Does not reject requests with a missing/invalid token itself; it simply
 * leaves the security context empty, and {@link SecurityConfig}'s
 * authorization rules (plus {@code RestAuthenticationEntryPoint}) are what
 * turn "no authentication" into a 401 for protected endpoints.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        extractToken(request).flatMap(jwtTokenProvider::parseAndValidate).ifPresent(principal -> {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
            principal.permissions()
                    .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getAuthority())));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return Optional.of(header.substring(PREFIX.length()));
        }
        return Optional.empty();
    }
}
