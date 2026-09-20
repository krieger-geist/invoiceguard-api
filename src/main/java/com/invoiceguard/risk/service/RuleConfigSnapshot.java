package com.invoiceguard.risk.service;

/** Cache-friendly value for {@code RiskRuleConfigService.getEffectiveConfigSnapshot} — deliberately not the JPA entity. */
public record RuleConfigSnapshot(boolean enabled, Integer weightPoints) {}
