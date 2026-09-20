package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MultipleBankChangesRule implements RiskRule {

    private static final int THRESHOLD = 2;

    private final RiskEngineProperties properties;

    public MultipleBankChangesRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.MULTIPLE_BANK_CHANGES;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (context.recentBankChangeCount() < THRESHOLD) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Multiple recent bank detail changes",
                "This vendor has changed bank details " + context.recentBankChangeCount() + " times in the last "
                        + properties.recentBankChangeWindowDays() + " days — an unusually high rate of change.",
                RiskFindingSeverity.HIGH,
                15,
                "{\"changeCount\":" + context.recentBankChangeCount() + "}"));
    }
}
