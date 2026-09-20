package com.invoiceguard.vendor.event;

import java.util.UUID;

public record VendorSuspendedEvent(UUID vendorId, UUID organizationId) {}
