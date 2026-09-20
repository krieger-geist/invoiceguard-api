package com.invoiceguard.approval.event;

import java.util.UUID;

public record InvoiceApprovedEvent(UUID invoiceId, UUID organizationId, UUID approvedBy) {}
