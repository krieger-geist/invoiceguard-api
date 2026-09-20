package com.invoiceguard.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code invoiceguard.jwt.*} configuration keys. */
@ConfigurationProperties(prefix = "invoiceguard.jwt")
public record JwtProperties(String secret, int accessTokenTtlMinutes, int refreshTokenTtlDays, String issuer) {}
