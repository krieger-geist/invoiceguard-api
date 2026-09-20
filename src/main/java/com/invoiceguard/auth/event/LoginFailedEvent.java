package com.invoiceguard.auth.event;

/**
 * Published after a failed login attempt. Consumed by the audit module and,
 * in a later phase, by alerting (REPEATED_LOGIN_FAILURE).
 */
public record LoginFailedEvent(String email, String ipAddress, String reason) {}
