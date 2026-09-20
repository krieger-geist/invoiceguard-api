package com.invoiceguard.approval.event;

import java.util.UUID;

public record InvoiceRejectedEvent(UUID invoiceId, UUID organizationId, UUID rejectedBy, String reason) {}
