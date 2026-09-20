package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Weak signal on its own — round-number invoices are common and legitimate — but contributes to the overall picture. */
@Component
public class RoundAmountPatternRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.ROUND_AMOUNT_PATTERN;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        BigDecimal amount = context.invoice().getTotalAmount();
        boolean isRound = amount.signum() > 0
                && amount.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) == 0;
        if (!isRound) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Suspiciously round invoice amount",
                "The total amount (" + amount + ") is an exact multiple of 100, a pattern sometimes seen in fabricated invoices.",
                RiskFindingSeverity.LOW,
                5,
                "{\"totalAmount\":\"" + amount + "\"}"));
    }
}
