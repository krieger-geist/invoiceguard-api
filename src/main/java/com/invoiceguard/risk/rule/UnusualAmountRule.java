package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Flags invoices that are statistical outliers for THIS vendor's historical amounts (z-score based). */
@Component
public class UnusualAmountRule implements RiskRule {

    private final RiskEngineProperties properties;

    public UnusualAmountRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.UNUSUAL_AMOUNT;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        var stats = context.vendorStatistics();
        if (stats.sampleSize() < properties.minStatisticalSampleSize() || stats.stdDevAmount() == 0) {
            return Optional.empty();
        }
        double amount = context.invoice().getTotalAmount().doubleValue();
        double zScore = Math.abs(amount - stats.averageAmount().doubleValue()) / stats.stdDevAmount();
        if (zScore < properties.unusualAmountZScoreThreshold()) {
            return Optional.empty();
        }
        int points = (int) Math.min(25, Math.round(zScore * 6));
        RiskFindingSeverity severity = zScore >= 4 ? RiskFindingSeverity.HIGH : RiskFindingSeverity.MEDIUM;
        return Optional.of(new RuleFinding(
                "Unusual invoice amount for this vendor",
                "This invoice's amount is %.1f standard deviations from this vendor's historical average of %s (based on %d prior invoices)."
                        .formatted(zScore, stats.averageAmount(), stats.sampleSize()),
                severity,
                points,
                "{\"zScore\":%.2f,\"average\":%s,\"stdDev\":%.2f}".formatted(zScore, stats.averageAmount(), stats.stdDevAmount())));
    }
}
