package com.invoiceguard.risk.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.service.VendorStatistics;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnusualAmountRuleTest {

    private final RiskEngineProperties properties = new RiskEngineProperties(
            new RiskEngineProperties.Thresholds(29, 59, 79),
            5, // minStatisticalSampleSize
            2.5, // unusualAmountZScoreThreshold
            5, 180, 8, 19, 90, 30, 5, List.of(), List.of());

    private final UnusualAmountRule rule = new UnusualAmountRule(properties);

    @Test
    void flagsAmountFarFromVendorAverage() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("50000"));

        VendorStatistics stats = new VendorStatistics(
                10, new BigDecimal("1000"), new BigDecimal("1000"), 200.0, new BigDecimal("800"), new BigDecimal("1200"), 0.1, null);

        RiskContext context = RiskContextFixtures.invoice(invoice).vendorStatistics(stats).build();

        assertThat(rule.evaluate(context)).isPresent();
    }

    @Test
    void doesNotFlagAmountCloseToVendorAverage() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("1050"));

        VendorStatistics stats = new VendorStatistics(
                10, new BigDecimal("1000"), new BigDecimal("1000"), 200.0, new BigDecimal("800"), new BigDecimal("1200"), 0.1, null);

        RiskContext context = RiskContextFixtures.invoice(invoice).vendorStatistics(stats).build();

        assertThat(rule.evaluate(context)).isEmpty();
    }

    @Test
    void doesNotFlagWhenSampleSizeTooSmall() {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal("50000"));

        // Only 2 prior invoices — below minStatisticalSampleSize of 5, so the rule should not fire
        // even though the amount would otherwise look like a huge outlier.
        VendorStatistics stats = new VendorStatistics(
                2, new BigDecimal("1000"), new BigDecimal("1000"), 200.0, new BigDecimal("800"), new BigDecimal("1200"), 0.1, null);

        RiskContext context = RiskContextFixtures.invoice(invoice).vendorStatistics(stats).build();

        assertThat(rule.evaluate(context)).isEmpty();
    }
}
