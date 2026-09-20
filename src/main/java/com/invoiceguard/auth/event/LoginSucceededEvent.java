package com.invoiceguard.auth.event;

import java.util.UUID;

/** Published after a successful login. Consumed by the audit module (later phase). */
public record LoginSucceededEvent(UUID userId, UUID organizationId, String ipAddress, String userAgent) {}
