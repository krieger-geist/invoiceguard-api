package com.invoiceguard.risk.dto;

import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.risk.entity.RecommendedAction;
import com.invoiceguard.risk.entity.RiskAssessmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessmentResponse(
        UUID id,
        UUID invoiceId,
        int totalRiskScore,
        InvoiceRiskLevel riskLevel,
        RecommendedAction recommendedAction,
        RiskAssessmentStatus status,
        String explanation,
        String aiSummary,
        String engineVersion,
        long processingDurationMs,
        Instant analysedAt,
        List<RiskFindingResponse> findings) {}
