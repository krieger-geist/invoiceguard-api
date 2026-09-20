package com.invoiceguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and parses short-lived JWT access tokens.
 *
 * <p>Claims carried: {@code sub} (userId), {@code email}, {@code orgId},
 * {@code role}, {@code perms} (permission authority strings). Refresh
 * tokens are intentionally NOT JWTs — see {@link com.invoiceguard.auth.entity.RefreshToken}
 * for why they're opaque, server-verified strings instead.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ORG_ID = "orgId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_PERMISSIONS = "perms";

    private final JwtProperties properties;
    private final Key signingKey;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtTokenProvider(JwtProperties properties, TokenBlacklistService tokenBlacklistService) {
        this.properties = properties;
        this.tokenBlacklistService = tokenBlacklistService;
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "invoiceguard.jwt.secret (JWT_SECRET) must be configured — refusing to start with no signing key");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);

        List<String> permissionAuthorities = principal.permissions().stream()
                .map(Permission::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(principal.userId().toString())
                .claim(CLAIM_EMAIL, principal.email())
                .claim(CLAIM_ORG_ID, principal.organizationId().toString())
                .claim(CLAIM_ROLE, principal.role().name())
                .claim(CLAIM_PERMISSIONS, permissionAuthorities)
                .issuer(properties.issuer())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .signWith((SecretKey) signingKey)
                .compact();
    }

    /** Returns the parsed principal, or empty if the token is invalid, malformed, expired, or blacklisted. */
    public java.util.Optional<AuthenticatedPrincipal> parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (tokenBlacklistService.isBlacklisted(claims.getId())) {
                return java.util.Optional.empty();
            }

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get(CLAIM_EMAIL, String.class);
            UUID organizationId = UUID.fromString(claims.get(CLAIM_ORG_ID, String.class));
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));

            @SuppressWarnings("unchecked")
            List<String> permissionAuthorities = claims.get(CLAIM_PERMISSIONS, List.class);
            Set<Permission> permissions = permissionAuthorities.stream()
                    .map(this::toPermission)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());

            return java.util.Optional.of(
                    new AuthenticatedPrincipal(userId, email, organizationId, role, permissions));
        } catch (JwtException e) {
            // ExpiredJwtException, malformed JWT, bad signature, unsupported — all invalid here.
        }
        return java.util.Optional.empty();
    }

    private Permission toPermission(String authority) {
        for (Permission permission : Permission.values()) {
            if (permission.getAuthority().equals(authority)) {
                return permission;
            }
        }
        return null;
    }

    public int accessTokenTtlMinutes() {
        return properties.accessTokenTtlMinutes();
    }

    /**
     * Called at logout to immediately revoke the access token that was used
     * to make the logout call, rather than letting it remain valid until its
     * natural expiry. Silently does nothing if the token is already
     * invalid/expired — nothing further to revoke in that case.
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            java.time.Duration remaining = java.time.Duration.between(Instant.now(), claims.getExpiration().toInstant());
            tokenBlacklistService.blacklist(claims.getId(), remaining);
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token already invalid/expired/malformed — nothing to blacklist.
        }
    }
}