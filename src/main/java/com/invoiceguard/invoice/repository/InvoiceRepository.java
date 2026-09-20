package com.invoiceguard.invoice.repository;

import com.invoiceguard.invoice.entity.Invoice;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** All historical invoices for a vendor — used by vendor statistics and several risk rules. */
    List<Invoice> findByOrganizationIdAndVendorIdOrderByInvoiceDateDesc(UUID organizationId, UUID vendorId);

    List<Invoice> findByOrganizationIdAndVendorIdAndNormalizedInvoiceNumber(
            UUID organizationId, UUID vendorId, String normalizedInvoiceNumber);

    List<Invoice> findByOrganizationIdAndVendorIdAndInvoiceDateBetween(
            UUID organizationId, UUID vendorId, LocalDate from, LocalDate to);

    List<Invoice> findByOrganizationIdAndPurchaseOrderNumberAndIdNot(
            UUID organizationId, String purchaseOrderNumber, UUID excludeInvoiceId);

    List<Invoice> findByOrganizationIdAndPaymentReferenceAndIdNot(
            UUID organizationId, String paymentReference, UUID excludeInvoiceId);

    List<Invoice> findByOrganizationIdAndVendorIdAndCreatedAtAfterAndIdNot(
            UUID organizationId, UUID vendorId, Instant after, UUID excludeInvoiceId);
}
