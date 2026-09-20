package com.invoiceguard.integration.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fields an OCR/document-extraction provider could plausibly recover from an invoice PDF or image. */
public record ExtractedInvoiceData(
        String invoiceNumber, LocalDate invoiceDate, BigDecimal totalAmount, String vendorNameGuess, double confidence) {}
