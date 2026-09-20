package com.invoiceguard.invoice.entity;

/**
 * Risk banding derived from {@code RiskAssessment.totalRiskScore} once the
 * risk engine (Phase 5) analyses an invoice. {@code null} until then —
 * there is no "UNSCORED" enum member; absence of a value already means that.
 */
public enum InvoiceRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
