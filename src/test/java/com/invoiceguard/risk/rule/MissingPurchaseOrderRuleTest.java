package com.invoiceguard.risk.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceguard.invoice.entity.Invoice;
import org.junit.jupiter.api.Test;

class MissingPurchaseOrderRuleTest {

    private final MissingPurchaseOrderRule rule = new MissingPurchaseOrderRule();

    @Test
    void flagsBlankPurchaseOrderNumber() {
        Invoice invoice = new Invoice();
        invoice.setPurchaseOrderNumber(null);
        assertThat(rule.evaluate(RiskContextFixtures.withInvoice(invoice))).isPresent();

        invoice.setPurchaseOrderNumber("   ");
        assertThat(rule.evaluate(RiskContextFixtures.withInvoice(invoice))).isPresent();
    }

    @Test
    void doesNotFlagWhenPurchaseOrderPresent() {
        Invoice invoice = new Invoice();
        invoice.setPurchaseOrderNumber("PO-5521");
        assertThat(rule.evaluate(RiskContextFixtures.withInvoice(invoice))).isEmpty();
    }
}
