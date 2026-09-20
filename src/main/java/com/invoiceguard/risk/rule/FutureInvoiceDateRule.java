package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FutureInvoiceDateRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.FUTURE_INVOICE_DATE;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (!context.invoice().getInvoiceDate().isAfter(LocalDate.now())) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Invoice dated in the future",
                "This invoice's date (" + context.invoice().getInvoiceDate() + ") is after today, which is unusual for a genuine invoice.",
                RiskFindingSeverity.MEDIUM,
                10,
                "{\"invoiceDate\":\"" + context.invoice().getInvoiceDate() + "\"}"));
    }
}
