package com.invoiceguard.integration.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code invoiceguard.ai.*}. {@code enabled=false} (the default) means
 * the application never makes an outbound AI call anywhere — every AI-backed
 * interface in this package has a fully-functional non-AI default, per the
 * spec's "the system must start and work even if no AI key is configured"
 * requirement.
 */
@ConfigurationProperties(prefix = "invoiceguard.ai")
public record AiProperties(boolean enabled, String provider, String apiKey, String model, int timeoutMs) {}
