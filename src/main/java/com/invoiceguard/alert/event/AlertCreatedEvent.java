package com.invoiceguard.alert.event;

import java.util.UUID;

public record AlertCreatedEvent(UUID alertId, UUID organizationId, String alertType, String severity, String title) {}
