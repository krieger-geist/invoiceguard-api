package com.invoiceguard.risk.rule;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceItem;
import com.invoiceguard.risk.service.DuplicateMatch;
import com.invoiceguard.risk.service.VendorStatistics;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorBankAccount;
import java.util.List;

/**
 * Assembled once per analysis run by {@code RiskEngine} and handed to every
 * {@link RiskRule} — this is what lets individual rules stay small, pure
 * functions of "given this context, is there a finding" with no repository
 * access of their own, no risk of one rule accidentally running an N+1 query
 * per invoice, and easy unit testing (just construct a context by hand).
 */
public record RiskContext(
        Invoice invoice,
        List<InvoiceItem> items,
        Vendor vendor,
        List<VendorBankAccount> vendorBankAccounts,
        int recentBankChangeCount,
        VendorStatistics vendorStatistics,
        List<DuplicateMatch> exactDuplicates,
        List<DuplicateMatch> nearDuplicates,
        boolean duplicatePurchaseOrderExists,
        boolean duplicatePaymentReferenceExists,
        boolean rapidRepeatSubmissionExists,
        boolean firstInvoiceForVendor,
        List<String> recentInvoiceDescriptions) {}
