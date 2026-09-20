package com.invoiceguard.invoice.dto;

import com.invoiceguard.common.enums.CurrencyCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceCreateRequest(
        @NotBlank @Size(max = 100) String invoiceNumber,
        @NotNull UUID vendorId,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        @NotNull CurrencyCode currency,
        @NotNull @DecimalMin(value = "0.0") BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0.0") BigDecimal taxAmount,
        @DecimalMin(value = "0.0") BigDecimal discountAmount,
        @NotNull @DecimalMin(value = "0.0") BigDecimal totalAmount,
        @Size(max = 100) String purchaseOrderNumber,
        @Size(max = 100) String paymentReference,
        @Size(max = 1000) String description,
        @Valid List<InvoiceItemRequest> items) {}
