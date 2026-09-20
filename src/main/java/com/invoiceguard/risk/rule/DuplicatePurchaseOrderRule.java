package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DuplicatePurchaseOrderRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.DUPLICATE_PURCHASE_ORDER;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (!context.duplicatePurchaseOrderExists()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Purchase order number reused",
                "Another invoice in this organisation already references purchase order '"
                        + context.invoice().getPurchaseOrderNumber() + "'. Verify this isn't a duplicate billing against the same PO.",
                RiskFindingSeverity.MEDIUM,
                15,
                "{\"purchaseOrderNumber\":\"" + context.invoice().getPurchaseOrderNumber() + "\"}"));
    }
}
