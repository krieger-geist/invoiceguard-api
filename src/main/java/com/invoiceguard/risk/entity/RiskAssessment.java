package com.invoiceguard.risk.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The outcome of one risk-engine run against one invoice. A new row is
 * written on every re-analysis (invoices are never re-scored in place),
 * so the assessment history for an invoice is a natural audit trail —
 * {@code RiskAssessmentRepository.findTopByInvoiceIdOrderByAnalysedAtDesc}
 * is what "the current score" means at any point in time.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "risk_assessments")
public class RiskAssessment extends OrganizationScopedEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "total_risk_score", nullable = false)
    private int totalRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private InvoiceRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 40)
    private RecommendedAction recommendedAction;

    @Column(name = "analysed_at", nullable = false)
    private Instant analysedAt;

    @Column(name = "engine_version", nullable = false, length = 20)
    private String engineVersion;

    @Column(name = "processing_duration_ms", nullable = false)
    private long processingDurationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RiskAssessmentStatus status = RiskAssessmentStatus.COMPLETED;

    /** Deterministic, rule-based explanation — always populated, never dependent on an external AI call. */
    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    /** Best-effort natural-language summary from the configured AI provider (Gemini). Null if AI is disabled or the call failed. */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
}
