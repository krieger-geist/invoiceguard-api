package com.invoiceguard.risk.rule;

import com.invoiceguard.common.util.StringSimilarity;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Flags an invoice description that has little in common with this vendor's typical invoice descriptions. */
@Component
public class DescriptionMismatchRule implements RiskRule {

    private static final int MIN_HISTORY_SAMPLE = 3;
    private static final double SIMILARITY_FLOOR = 0.1;

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.DESCRIPTION_MISMATCH;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        String description = context.invoice().getDescription();
        var history = context.recentInvoiceDescriptions();
        if (description == null || description.isBlank() || history.size() < MIN_HISTORY_SAMPLE) {
            return Optional.empty();
        }

        double bestSimilarity = history.stream()
                .mapToDouble(past -> StringSimilarity.wordOverlapSimilarity(description, past))
                .max()
                .orElse(0);
        if (bestSimilarity >= SIMILARITY_FLOOR) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Description differs from vendor's usual invoices",
                "This invoice's description has little in common with this vendor's previous invoice descriptions.",
                RiskFindingSeverity.LOW,
                8,
                "{\"bestSimilarity\":%.2f}".formatted(bestSimilarity)));
    }
}
