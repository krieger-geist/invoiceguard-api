package com.invoiceguard.vendor.event;

import java.util.UUID;

public record VendorBankChangeRequestedEvent(UUID vendorId, UUID organizationId, UUID bankChangeRequestId, UUID requestedBy) {}
