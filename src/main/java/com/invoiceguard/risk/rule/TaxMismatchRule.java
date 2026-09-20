package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Flags an invoice whose effective tax rate deviates sharply from this vendor's historical average. */
@Component
public class TaxMismatchRule implements RiskRule {

    private final RiskEngineProperties properties;

    public TaxMismatchRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.TAX_MISMATCH;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        var stats = context.vendorStatistics();
        if (stats.sampleSize() < properties.minStatisticalSampleSize()) {
            return Optional.empty();
        }
        var invoice = context.invoice();
        if (invoice.getSubtotal().signum() <= 0) {
            return Optional.empty();
        }
        double effectiveRate = invoice.getTaxAmount().doubleValue() / invoice.getSubtotal().doubleValue();
        double diffPercentagePoints = Math.abs(effectiveRate - stats.averageTaxRate()) * 100;
        if (diffPercentagePoints < properties.taxMismatchTolerancePercentagePoints()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Tax rate differs from vendor's historical pattern",
                "This invoice's effective tax rate (%.1f%%) differs from this vendor's historical average (%.1f%%) by more than expected."
                        .formatted(effectiveRate * 100, stats.averageTaxRate() * 100),
                RiskFindingSeverity.MEDIUM,
                10,
                "{\"effectiveRate\":%.4f,\"historicalAverage\":%.4f}".formatted(effectiveRate, stats.averageTaxRate())));
    }
}
