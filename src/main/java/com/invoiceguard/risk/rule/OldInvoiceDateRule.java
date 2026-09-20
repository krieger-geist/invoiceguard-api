package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OldInvoiceDateRule implements RiskRule {

    private final RiskEngineProperties properties;

    public OldInvoiceDateRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.OLD_INVOICE_DATE;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        long ageDays = ChronoUnit.DAYS.between(context.invoice().getInvoiceDate(), LocalDate.now());
        if (ageDays < properties.oldInvoiceDateDays()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Stale invoice date",
                "This invoice is dated " + ageDays + " days ago, well beyond typical submission timeframes.",
                RiskFindingSeverity.LOW,
                8,
                "{\"ageDays\":" + ageDays + "}"));
    }
}
