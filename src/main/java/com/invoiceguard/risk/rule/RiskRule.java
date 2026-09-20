package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;

/**
 * One deterministic risk check. Every implementation is a Spring
 * {@code @Component}, auto-discovered and run by {@code RiskEngine} via
 * {@code List<RiskRule>} injection — adding a new rule to the engine never
 * requires touching {@code RiskEngine} itself, only writing the new class.
 */
public interface RiskRule {

    RiskRuleCode getCode();

    Optional<RuleFinding> evaluate(RiskContext context);
}
