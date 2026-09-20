package com.invoiceguard.auth.event;

import java.util.UUID;

/** Published after a new user + organisation are successfully created. */
public record UserRegisteredEvent(UUID userId, UUID organizationId, String email) {}
