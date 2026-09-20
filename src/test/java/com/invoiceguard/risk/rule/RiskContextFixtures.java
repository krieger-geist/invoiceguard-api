package com.invoiceguard.risk.rule;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceItem;
import com.invoiceguard.risk.service.DuplicateMatch;
import com.invoiceguard.risk.service.VendorStatistics;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorBankAccount;
import java.time.Instant;
import java.util.List;

/**
 * Builds a {@link RiskContext} with sensible defaults for everything a test
 * doesn't care about, so each rule's unit test only needs to set the one or
 * two fields its logic actually depends on. Test-only — lives alongside the
 * rules it supports rather than in {@code common}, since no production code
 * should ever need to construct a context piecemeal like this (that's
 * {@code RiskEngine.buildContext}'s job).
 */
public final class RiskContextFixtures {

    private Invoice invoice;
    private List<InvoiceItem> items = List.of();
    private Vendor vendor = new Vendor();
    private List<VendorBankAccount> vendorBankAccounts = List.of();
    private int recentBankChangeCount = 0;
    private VendorStatistics vendorStatistics = VendorStatistics.empty();
    private List<DuplicateMatch> exactDuplicates = List.of();
    private List<DuplicateMatch> nearDuplicates = List.of();
    private boolean duplicatePurchaseOrderExists = false;
    private boolean duplicatePaymentReferenceExists = false;
    private boolean rapidRepeatSubmissionExists = false;
    private boolean firstInvoiceForVendor = false;
    private List<String> recentInvoiceDescriptions = List.of();

    private RiskContextFixtures(Invoice invoice) {
        this.invoice = invoice;
        if (invoice.getCreatedAt() == null) {
            invoice.setCreatedAt(Instant.parse("2026-06-10T14:30:00Z")); // a Wednesday, mid-afternoon UTC
        }
        if (invoice.getInvoiceDate() == null) {
            invoice.setInvoiceDate(java.time.LocalDate.of(2026, 6, 10));
        }
        if (invoice.getTotalAmount() == null) {
            invoice.setTotalAmount(java.math.BigDecimal.valueOf(100));
        }
        vendor.setVerificationStatus(com.invoiceguard.vendor.entity.VendorVerificationStatus.VERIFIED);
        vendor.setCountry("US");
        vendor.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
    }

    public static RiskContext withInvoice(Invoice invoice) {
        return new RiskContextFixtures(invoice).build();
    }

    public static Builder invoice(Invoice invoice) {
        return new Builder(new RiskContextFixtures(invoice));
    }

    private RiskContext build() {
        return new RiskContext(
                invoice, items, vendor, vendorBankAccounts, recentBankChangeCount, vendorStatistics, exactDuplicates,
                nearDuplicates, duplicatePurchaseOrderExists, duplicatePaymentReferenceExists,
                rapidRepeatSubmissionExists, firstInvoiceForVendor, recentInvoiceDescriptions);
    }

    /** Fluent builder for tests that need to override more than just the invoice. */
    public static final class Builder {
        private final RiskContextFixtures fixtures;

        private Builder(RiskContextFixtures fixtures) {
            this.fixtures = fixtures;
        }

        public Builder vendor(Vendor vendor) {
            fixtures.vendor = vendor;
            return this;
        }

        public Builder vendorStatistics(VendorStatistics statistics) {
            fixtures.vendorStatistics = statistics;
            return this;
        }

        public Builder exactDuplicates(List<DuplicateMatch> matches) {
            fixtures.exactDuplicates = matches;
            return this;
        }

        public Builder nearDuplicates(List<DuplicateMatch> matches) {
            fixtures.nearDuplicates = matches;
            return this;
        }

        public Builder recentBankChangeCount(int count) {
            fixtures.recentBankChangeCount = count;
            return this;
        }

        public Builder duplicatePurchaseOrderExists(boolean value) {
            fixtures.duplicatePurchaseOrderExists = value;
            return this;
        }

        public Builder duplicatePaymentReferenceExists(boolean value) {
            fixtures.duplicatePaymentReferenceExists = value;
            return this;
        }

        public Builder rapidRepeatSubmissionExists(boolean value) {
            fixtures.rapidRepeatSubmissionExists = value;
            return this;
        }

        public Builder firstInvoiceForVendor(boolean value) {
            fixtures.firstInvoiceForVendor = value;
            return this;
        }

        public Builder recentInvoiceDescriptions(List<String> descriptions) {
            fixtures.recentInvoiceDescriptions = descriptions;
            return this;
        }

        public RiskContext build() {
            return fixtures.build();
        }
    }
}
