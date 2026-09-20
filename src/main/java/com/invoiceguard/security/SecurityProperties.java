package com.invoiceguard.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code invoiceguard.security.*} configuration keys. */
@ConfigurationProperties(prefix = "invoiceguard.security")
public record SecurityProperties(int maxFailedLoginAttempts, int accountLockMinutes) {}
