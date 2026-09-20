package com.invoiceguard.analytics.dto;

import java.math.BigDecimal;

public record MonthlyTrendResponse(String yearMonth, long invoiceCount, BigDecimal totalAmount) {}
