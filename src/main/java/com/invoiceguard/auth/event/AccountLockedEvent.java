package com.invoiceguard.auth.event;

import java.util.UUID;

public record AccountLockedEvent(UUID userId, String email, String ipAddress) {}
