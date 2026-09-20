package com.invoiceguard.risk.event;

import com.invoiceguard.risk.entity.RecommendedAction;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import java.util.UUID;

public record InvoiceAnalysedEvent(
        UUID invoiceId,
        UUID organizationId,
        int totalRiskScore,
        InvoiceRiskLevel riskLevel,
        RecommendedAction recommendedAction,
        boolean hasExactDuplicate) {}
