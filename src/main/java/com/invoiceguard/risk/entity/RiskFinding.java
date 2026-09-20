package com.invoiceguard.risk.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
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
 * One rule's contribution to a {@link RiskAssessment} — this is what makes
 * the engine's output explainable rather than a bare number: every point
 * in {@code RiskAssessment.totalRiskScore} traces back to a specific,
 * human-readable finding here.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "risk_findings")
public class RiskFinding extends OrganizationScopedEntity {

    @Column(name = "risk_assessment_id", nullable = false)
    private UUID riskAssessmentId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_code", nullable = false, length = 40)
    private RiskRuleCode ruleCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private RiskFindingSeverity severity;

    @Column(name = "points", nullable = false)
    private int points;

    /** Free-form JSON string capturing the concrete data that triggered the rule (matched invoice IDs, amounts, etc). */
    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
