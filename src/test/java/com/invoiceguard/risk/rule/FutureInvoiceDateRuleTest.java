package com.invoiceguard.risk.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceguard.invoice.entity.Invoice;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FutureInvoiceDateRuleTest {

    private final FutureInvoiceDateRule rule = new FutureInvoiceDateRule();

    @Test
    void flagsInvoiceDatedTomorrow() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(LocalDate.now().plusDays(1));
        RiskContext context = RiskContextFixtures.withInvoice(invoice);

        assertThat(rule.evaluate(context)).isPresent();
    }

    @Test
    void doesNotFlagTodayOrPastDates() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(LocalDate.now());
        RiskContext context = RiskContextFixtures.withInvoice(invoice);
        assertThat(rule.evaluate(context)).isEmpty();

        invoice.setInvoiceDate(LocalDate.now().minusDays(3));
        assertThat(rule.evaluate(RiskContextFixtures.withInvoice(invoice))).isEmpty();
    }
}
