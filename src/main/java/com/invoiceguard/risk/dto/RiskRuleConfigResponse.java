package com.invoiceguard.risk.dto;

import com.invoiceguard.risk.entity.RiskRuleCode;

public record RiskRuleConfigResponse(RiskRuleCode ruleCode, boolean enabled, Integer weightPoints, boolean usingDefaultWeight) {}
