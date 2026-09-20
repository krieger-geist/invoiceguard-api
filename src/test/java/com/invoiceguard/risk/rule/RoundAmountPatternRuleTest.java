package com.invoiceguard.risk.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceguard.invoice.entity.Invoice;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RoundAmountPatternRuleTest {

    private final RoundAmountPatternRule rule = new RoundAmountPatternRule();

    @Test
    void flagsExactHundredMultiples() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("5000.00"));
        RiskContext context = RiskContextFixtures.withInvoice(invoice);

        assertThat(rule.evaluate(context)).isPresent();
    }

    @Test
    void doesNotFlagOrdinaryAmounts() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("4837.42"));
        RiskContext context = RiskContextFixtures.withInvoice(invoice);

        assertThat(rule.evaluate(context)).isEmpty();
    }

    @Test
    void doesNotFlagZeroAmount() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(BigDecimal.ZERO);
        RiskContext context = RiskContextFixtures.withInvoice(invoice);

        assertThat(rule.evaluate(context)).isEmpty();
    }
}
