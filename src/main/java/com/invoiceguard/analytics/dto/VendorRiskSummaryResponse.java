package com.invoiceguard.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorRiskSummaryResponse(
        UUID vendorId, String vendorCode, String riskStatus, long invoiceCount, BigDecimal totalAmount) {}
