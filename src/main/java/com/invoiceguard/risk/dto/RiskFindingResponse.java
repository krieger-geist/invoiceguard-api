package com.invoiceguard.risk.dto;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.time.Instant;
import java.util.UUID;

public record RiskFindingResponse(
        UUID id,
        RiskRuleCode ruleCode,
        String title,
        String description,
        RiskFindingSeverity severity,
        int points,
        String evidence,
        boolean resolved,
        Instant resolvedAt) {}
