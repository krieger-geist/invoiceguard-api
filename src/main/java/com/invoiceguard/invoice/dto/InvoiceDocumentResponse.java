package com.invoiceguard.invoice.dto;

import java.time.Instant;
import java.util.UUID;

public record InvoiceDocumentResponse(
        UUID id, String originalFilename, String mimeType, long sizeBytes, String checksum, Instant createdAt) {}
