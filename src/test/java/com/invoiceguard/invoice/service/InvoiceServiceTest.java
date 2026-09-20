package com.invoiceguard.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.invoiceguard.common.enums.CurrencyCode;
import com.invoiceguard.exception.BusinessRuleViolationException;
import com.invoiceguard.exception.InvalidStateTransitionException;
import com.invoiceguard.invoice.dto.InvoiceCreateRequest;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import com.invoiceguard.invoice.repository.InvoiceItemRepository;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import com.invoiceguard.invoice.repository.InvoiceStatusHistoryRepository;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.service.VendorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceItemRepository invoiceItemRepository;
    @Mock private InvoiceStatusHistoryRepository statusHistoryRepository;
    @Mock private VendorService vendorService;
    @Mock private IdempotencyService idempotencyService;
    @Mock private TenantContext tenantContext;
    @Mock private ApplicationEventPublisher eventPublisher;

    private InvoiceService invoiceService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                invoiceRepository, invoiceItemRepository, statusHistoryRepository, vendorService, idempotencyService,
                tenantContext, eventPublisher);
    }

    private InvoiceCreateRequest requestWithTotals(BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total) {
        return new InvoiceCreateRequest(
                "INV-1001", vendorId, LocalDate.of(2026, 6, 1), null, CurrencyCode.USD, subtotal, tax, discount, total,
                null, null, null, null);
    }

    @Test
    void rejectsInvoiceWhenTotalsDoNotReconcile() {
        lenient().when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setOrganizationId(orgId);
        lenient().when(vendorService.getOwnedById(vendorId)).thenReturn(vendor);

        InvoiceCreateRequest request = requestWithTotals(
                new BigDecimal("100.00"), new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("200.00"));

        assertThatThrownBy(() -> invoiceService.create(request, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("totalAmount");
    }

    @Test
    void acceptsInvoiceWhenTotalsReconcileWithinTolerance() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setOrganizationId(orgId);
        when(vendorService.getOwnedById(vendorId)).thenReturn(vendor);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });

        // subtotal + tax - discount = 100 + 10 - 0 = 110, matches total exactly.
        InvoiceCreateRequest request = requestWithTotals(
                new BigDecimal("100.00"), new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("110.00"));

        InvoiceService.InvoiceCreationOutcome outcome = invoiceService.create(request, null);

        assertThat(outcome.invoice().getNormalizedInvoiceNumber()).isEqualTo("INV1001");
        assertThat(outcome.invoice().getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(outcome.httpStatus()).isEqualTo(201);
    }

    @Test
    void allowsOneCentRoundingTolerance() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setOrganizationId(orgId);
        when(vendorService.getOwnedById(vendorId)).thenReturn(vendor);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId(UUID.randomUUID());
            return invoice;
        });

        // 100 + 10 - 0 = 110.00, total given as 110.01 — exactly at the 1-cent tolerance boundary.
        InvoiceCreateRequest request = requestWithTotals(
                new BigDecimal("100.00"), new BigDecimal("10.00"), BigDecimal.ZERO, new BigDecimal("110.01"));

        assertThat(invoiceService.create(request, null)).isNotNull();
    }

    @Test
    void allowsSubmittedToAnalysingTransition() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganizationId(orgId);
        invoice.setStatus(InvoiceStatus.SUBMITTED);

        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(invoiceRepository.findByIdAndOrganizationId(invoice.getId(), orgId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice result = invoiceService.transition(invoice.getId(), InvoiceStatus.ANALYSING, "test");

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.ANALYSING);
    }

    @Test
    void rejectsIllegalStatusTransition() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganizationId(orgId);
        invoice.setStatus(InvoiceStatus.APPROVED);

        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(invoiceRepository.findByIdAndOrganizationId(invoice.getId(), orgId)).thenReturn(Optional.of(invoice));

        // APPROVED can only move to PAID or ARCHIVED — DRAFT is not a legal target from here.
        assertThatThrownBy(() -> invoiceService.transition(invoice.getId(), InvoiceStatus.DRAFT, "test"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
