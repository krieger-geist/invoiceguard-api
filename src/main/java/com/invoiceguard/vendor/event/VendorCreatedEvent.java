package com.invoiceguard.vendor.event;

import java.util.UUID;

public record VendorCreatedEvent(UUID vendorId, UUID organizationId, String vendorCode) {}
