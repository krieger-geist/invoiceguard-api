package com.invoiceguard.invoice.dto;

import com.invoiceguard.invoice.entity.InvoiceStatus;
import java.time.Instant;
import java.util.UUID;

public record InvoiceStatusHistoryResponse(
        UUID id, InvoiceStatus fromStatus, InvoiceStatus toStatus, String reason, String changedBy, Instant changedAt) {}
