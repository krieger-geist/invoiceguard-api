package com.invoiceguard.analytics.dto;

import java.util.List;
import java.util.Map;

public record RiskDistributionResponse(Map<String, Long> byRiskLevel, List<RuleFrequency> topRiskRules) {

    public record RuleFrequency(String ruleCode, long count) {}
}
