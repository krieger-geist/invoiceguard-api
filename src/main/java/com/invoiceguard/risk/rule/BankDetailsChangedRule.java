package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BankDetailsChangedRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.BANK_DETAILS_CHANGED;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (context.recentBankChangeCount() < 1) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Vendor bank details recently changed",
                "This vendor's payment bank account was changed recently. Verify the change was legitimate before payment.",
                RiskFindingSeverity.MEDIUM,
                20,
                "{\"recentBankChangeCount\":" + context.recentBankChangeCount() + "}"));
    }
}
