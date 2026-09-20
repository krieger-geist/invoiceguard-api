package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ExactDuplicateRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.EXACT_DUPLICATE;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (context.exactDuplicates().isEmpty()) {
            return Optional.empty();
        }
        var match = context.exactDuplicates().get(0);
        return Optional.of(new RuleFinding(
                "Exact duplicate invoice detected",
                "This invoice is identical to a previously submitted invoice (" + match.explanation() + ").",
                RiskFindingSeverity.CRITICAL,
                40,
                "{\"matchedInvoiceId\":\"" + match.matchedInvoiceId() + "\",\"matchCount\":"
                        + context.exactDuplicates().size() + "}"));
    }
}
