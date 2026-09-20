package com.invoiceguard.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0") BigDecimal unitPrice,
        @DecimalMin(value = "0.0") BigDecimal taxRate,
        @Size(max = 100) String category,
        @Size(max = 100) String productCode) {}
