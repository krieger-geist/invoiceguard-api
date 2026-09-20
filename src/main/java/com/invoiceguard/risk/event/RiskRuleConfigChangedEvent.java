package com.invoiceguard.risk.event;

import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.UUID;

public record RiskRuleConfigChangedEvent(UUID organizationId, RiskRuleCode ruleCode, boolean enabled, Integer weightPoints, UUID changedBy) {}
