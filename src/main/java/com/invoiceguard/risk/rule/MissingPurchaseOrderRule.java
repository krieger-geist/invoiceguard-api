package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MissingPurchaseOrderRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.MISSING_PURCHASE_ORDER;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        String po = context.invoice().getPurchaseOrderNumber();
        if (po != null && !po.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "No purchase order reference",
                "This invoice does not reference a purchase order number, making three-way match verification impossible.",
                RiskFindingSeverity.LOW,
                8,
                "{}"));
    }
}
