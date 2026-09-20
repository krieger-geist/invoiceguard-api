package com.invoiceguard.invoice.event;

import java.util.UUID;

public record InvoiceSubmittedEvent(UUID invoiceId, UUID organizationId, UUID submittedBy) {}
