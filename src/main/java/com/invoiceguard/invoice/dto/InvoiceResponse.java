package com.invoiceguard.invoice.dto;

import com.invoiceguard.common.enums.CurrencyCode;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID vendorId,
        LocalDate invoiceDate,
        LocalDate dueDate,
        CurrencyCode currency,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String purchaseOrderNumber,
        String paymentReference,
        String description,
        InvoiceStatus status,
        InvoiceRiskLevel riskLevel,
        Integer latestRiskScore,
        List<InvoiceItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {}
