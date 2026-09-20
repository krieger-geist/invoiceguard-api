package com.invoiceguard.risk.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.service.DuplicateMatch;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExactDuplicateRuleTest {

    private final ExactDuplicateRule rule = new ExactDuplicateRule();

    @Test
    void flagsCriticalWhenExactDuplicateExists() {
        DuplicateMatch match = new DuplicateMatch(
                UUID.randomUUID(), "EXACT", 100, List.of("vendor", "totalAmount"), "identical invoice", new BigDecimal("500.00"));
        RiskContext context = RiskContextFixtures.invoice(new Invoice()).exactDuplicates(List.of(match)).build();

        var finding = rule.evaluate(context);
        assertThat(finding).isPresent();
        assertThat(finding.get().severity()).isEqualTo(RiskFindingSeverity.CRITICAL);
        assertThat(finding.get().basePoints()).isEqualTo(40);
    }

    @Test
    void doesNotFlagWhenNoExactDuplicates() {
        RiskContext context = RiskContextFixtures.withInvoice(new Invoice());
        assertThat(rule.evaluate(context)).isEmpty();
    }
}
