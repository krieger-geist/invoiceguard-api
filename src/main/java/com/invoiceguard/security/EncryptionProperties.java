package com.invoiceguard.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code invoiceguard.security.bank-account-encryption-key}. */
@ConfigurationProperties(prefix = "invoiceguard.security")
public record EncryptionProperties(String bankAccountEncryptionKey) {}
