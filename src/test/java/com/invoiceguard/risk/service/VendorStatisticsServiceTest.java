package com.invoiceguard.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorStatisticsServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private final UUID orgId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    private Invoice invoiceWithAmount(String amount, String tax, String subtotal) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTotalAmount(new BigDecimal(amount));
        invoice.setTaxAmount(new BigDecimal(tax));
        invoice.setSubtotal(new BigDecimal(subtotal));
        invoice.setInvoiceDate(LocalDate.of(2026, 1, 1));
        invoice.setCreatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        return invoice;
    }

    @Test
    void returnsEmptyStatisticsWhenVendorHasNoHistory() {
        when(invoiceRepository.findByOrganizationIdAndVendorIdOrderByInvoiceDateDesc(orgId, vendorId)).thenReturn(List.of());

        VendorStatisticsService service = new VendorStatisticsService(invoiceRepository);
        VendorStatistics stats = service.computeFor(orgId, vendorId, null);

        assertThat(stats.sampleSize()).isZero();
    }

    @Test
    void computesMeanMedianAndStdDevCorrectly() {
        // Amounts: 100, 200, 300, 400, 500 -> mean=300, population stddev = sqrt(20000)=141.42, median=300
        List<Invoice> history = List.of(
                invoiceWithAmount("100", "10", "90"),
                invoiceWithAmount("200", "10", "190"),
                invoiceWithAmount("300", "10", "290"),
                invoiceWithAmount("400", "10", "390"),
                invoiceWithAmount("500", "10", "490"));
        when(invoiceRepository.findByOrganizationIdAndVendorIdOrderByInvoiceDateDesc(orgId, vendorId)).thenReturn(history);

        VendorStatisticsService service = new VendorStatisticsService(invoiceRepository);
        VendorStatistics stats = service.computeFor(orgId, vendorId, null);

        assertThat(stats.sampleSize()).isEqualTo(5);
        assertThat(stats.averageAmount()).isEqualByComparingTo("300.00");
        assertThat(stats.medianAmount()).isEqualByComparingTo("300");
        assertThat(stats.stdDevAmount()).isCloseTo(141.42, within(0.1));
        assertThat(stats.minAmount()).isEqualByComparingTo("100");
        assertThat(stats.maxAmount()).isEqualByComparingTo("500");
    }

    @Test
    void excludesTheInvoiceBeingAnalysedFromItsOwnStatistics() {
        Invoice current = invoiceWithAmount("999999", "0", "999999");
        List<Invoice> history = List.of(invoiceWithAmount("100", "10", "90"), current);
        when(invoiceRepository.findByOrganizationIdAndVendorIdOrderByInvoiceDateDesc(orgId, vendorId)).thenReturn(history);

        VendorStatisticsService service = new VendorStatisticsService(invoiceRepository);
        VendorStatistics stats = service.computeFor(orgId, vendorId, current.getId());

        assertThat(stats.sampleSize()).isEqualTo(1);
        assertThat(stats.averageAmount()).isEqualByComparingTo("100.00");
    }
}
