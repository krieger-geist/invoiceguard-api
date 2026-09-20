package com.invoiceguard.invoice.util;

import java.util.regex.Pattern;

/**
 * Normalizes an invoice number so that "INV-1001", "inv 1001", and "INV_1001"
 * are all recognized as the same number for comparison purposes. Used at
 * invoice-creation time (this phase) and again by the duplicate-detection
 * engine (Phase 5) — kept as one static utility precisely so both call sites
 * can never drift out of sync with each other.
 */
public final class InvoiceNumberNormalizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]");

    private InvoiceNumberNormalizer() {}

    public static String normalize(String rawInvoiceNumber) {
        if (rawInvoiceNumber == null) {
            return "";
        }
        String upper = rawInvoiceNumber.toUpperCase();
        return NON_ALPHANUMERIC.matcher(upper).replaceAll("");
    }
}
