package com.invoiceguard.invoice.dto;

import com.invoiceguard.common.enums.CurrencyCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceUpdateRequest(
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
