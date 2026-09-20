package com.invoiceguard.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(
        long totalInvoices,
        long approvedInvoices,
        long rejectedInvoices,
        long underReviewInvoices,
        long pendingApprovalCount,
        long overdueApprovalCount,
        long duplicateInvoicesFound,
        BigDecimal preventedDuplicateAmount,
        BigDecimal estimatedPreventedLoss,
        Double averageApprovalTimeHours) {}
