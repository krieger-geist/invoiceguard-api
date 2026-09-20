package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UnknownVendorRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.UNKNOWN_VENDOR;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (!context.firstInvoiceForVendor()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "First invoice from this vendor",
                "No invoicing history exists for this vendor yet — there is no historical baseline to compare this invoice against.",
                RiskFindingSeverity.LOW,
                10,
                "{}"));
    }
}
