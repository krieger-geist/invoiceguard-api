package com.invoiceguard.invoice.entity;

/** Full invoice lifecycle. See {@code InvoiceService} for the allowed-transition map. */
public enum InvoiceStatus {
    DRAFT,
    SUBMITTED,
    ANALYSING,
    REVIEW_REQUIRED,
    APPROVED,
    REJECTED,
    PAID,
    CANCELLED,
    ARCHIVED
}
