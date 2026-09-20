package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;

/**
 * What a {@link RiskRule} reports when it fires. {@code basePoints} is the
 * rule's own default weight — {@code RiskEngine} applies any
 * organisation-specific override from {@code RiskRuleConfiguration} on top
 * of this before persisting the final {@code RiskFinding.points}.
 */
public record RuleFinding(
        String title, String description, RiskFindingSeverity severity, int basePoints, String evidenceJson) {}
