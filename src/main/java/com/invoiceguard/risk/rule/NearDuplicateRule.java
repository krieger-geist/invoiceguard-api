package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import com.invoiceguard.risk.service.DuplicateMatch;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NearDuplicateRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.NEAR_DUPLICATE;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (context.nearDuplicates().isEmpty()) {
            return Optional.empty();
        }
        DuplicateMatch best = context.nearDuplicates().stream()
                .max(Comparator.comparingInt(DuplicateMatch::confidenceScore))
                .orElseThrow();
        int points = Math.min(25, best.confidenceScore() / 4);
        RiskFindingSeverity severity = best.confidenceScore() >= 85 ? RiskFindingSeverity.HIGH : RiskFindingSeverity.MEDIUM;
        return Optional.of(new RuleFinding(
                "Possible near-duplicate invoice",
                best.explanation() + " (confidence " + best.confidenceScore() + "%).",
                severity,
                points,
                "{\"matchedInvoiceId\":\"" + best.matchedInvoiceId() + "\",\"confidence\":" + best.confidenceScore() + "}"));
    }
}
