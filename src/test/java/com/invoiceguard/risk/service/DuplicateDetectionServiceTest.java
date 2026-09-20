package com.invoiceguard.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private DuplicateDetectionService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DuplicateDetectionService(invoiceRepository, 60, 30, 2);
    }

    private Invoice invoice(String invoiceNumber, String normalized, BigDecimal amount, LocalDate date) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganizationId(orgId);
        invoice.setVendorId(vendorId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setNormalizedInvoiceNumber(normalized);
        invoice.setTotalAmount(amount);
        invoice.setInvoiceDate(date);
        return invoice;
    }

    @Test
    void findsExactDuplicateWhenAmountAndDateMatch() {
        Invoice newInvoice = invoice("INV-1001", "INV1001", new BigDecimal("500.00"), LocalDate.of(2026, 6, 1));
        Invoice existing = invoice("INV-1001", "INV1001", new BigDecimal("500.00"), LocalDate.of(2026, 6, 1));

        when(invoiceRepository.findByOrganizationIdAndVendorIdAndNormalizedInvoiceNumber(orgId, vendorId, "INV1001"))
                .thenReturn(List.of(existing));

        List<DuplicateMatch> matches = service.findExactDuplicates(newInvoice);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).matchType()).isEqualTo("EXACT");
        assertThat(matches.get(0).confidenceScore()).isEqualTo(100);
    }

    @Test
    void doesNotFlagExactDuplicateWhenAmountDiffers() {
        Invoice newInvoice = invoice("INV-1001", "INV1001", new BigDecimal("500.00"), LocalDate.of(2026, 6, 1));
        Invoice existing = invoice("INV-1001", "INV1001", new BigDecimal("650.00"), LocalDate.of(2026, 6, 1));

        when(invoiceRepository.findByOrganizationIdAndVendorIdAndNormalizedInvoiceNumber(orgId, vendorId, "INV1001"))
                .thenReturn(List.of(existing));

        assertThat(service.findExactDuplicates(newInvoice)).isEmpty();
    }

    @Test
    void excludesSelfWhenCheckingForDuplicates() {
        Invoice newInvoice = invoice("INV-1001", "INV1001", new BigDecimal("500.00"), LocalDate.of(2026, 6, 1));

        // Simulate the repository returning the invoice itself among candidates (e.g. re-analysis).
        when(invoiceRepository.findByOrganizationIdAndVendorIdAndNormalizedInvoiceNumber(orgId, vendorId, "INV1001"))
                .thenReturn(List.of(newInvoice));

        assertThat(service.findExactDuplicates(newInvoice)).isEmpty();
    }

    @Test
    void findsNearDuplicateWithSimilarInvoiceNumberAndCloseAmount() {
        Invoice newInvoice = invoice("INV-1001", "INV1001", new BigDecimal("1000.00"), LocalDate.of(2026, 6, 15));
        Invoice existing = invoice("INV-1OO1", "INV1OO1", new BigDecimal("1005.00"), LocalDate.of(2026, 6, 10));

        when(invoiceRepository.findByOrganizationIdAndVendorIdAndInvoiceDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        List<DuplicateMatch> matches = service.findNearDuplicates(newInvoice);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).matchType()).isEqualTo("NEAR");
        assertThat(matches.get(0).confidenceScore()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void doesNotFlagUnrelatedInvoicesAsNearDuplicates() {
        Invoice newInvoice = invoice("INV-1001", "INV1001", new BigDecimal("1000.00"), LocalDate.of(2026, 6, 15));
        Invoice existing = invoice("INV-9999", "INV9999", new BigDecimal("50.00"), LocalDate.of(2026, 6, 2));

        when(invoiceRepository.findByOrganizationIdAndVendorIdAndInvoiceDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThat(service.findNearDuplicates(newInvoice)).isEmpty();
    }
}
