package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PaymentReferenceReusedRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.PAYMENT_REFERENCE_REUSED;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (!context.duplicatePaymentReferenceExists()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Payment reference already used",
                "Another invoice already uses payment reference '" + context.invoice().getPaymentReference()
                        + "'. This may indicate an attempt to trigger a duplicate payment.",
                RiskFindingSeverity.HIGH,
                15,
                "{\"paymentReference\":\"" + context.invoice().getPaymentReference() + "\"}"));
    }
}
