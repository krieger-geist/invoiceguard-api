package com.invoiceguard.risk.service;

import com.invoiceguard.common.util.StringSimilarity;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Business-rule-first duplicate detection, run before any AI involvement —
 * per the spec, deterministic rules are the source of truth here and AI is
 * never consulted for this decision at all.
 *
 * <p><b>Exact duplicate:</b> same vendor, same normalized invoice number,
 * same total amount, same invoice date. (Document-hash comparison is folded
 * in when a hash is available, but isn't required — most invoices in this
 * phase won't have one yet, since OCR-based hashing is part of the
 * document-extraction AI integration, not this deterministic engine.)
 *
 * <p><b>Near duplicate:</b> a weighted combination of near-identical invoice
 * numbers (edit distance), close amounts within a date window, similar PO
 * numbers, and similar descriptions — scored 0-100 and reported only above
 * {@code invoiceguard.risk.duplicate.near-duplicate-threshold}.
 */
@Service
public class DuplicateDetectionService {

    private final InvoiceRepository invoiceRepository;
    private final int nearDuplicateThreshold;
    private final int nearDuplicateWindowDays;
    private final BigDecimal nearDuplicateAmountTolerancePercent;

    public DuplicateDetectionService(
            InvoiceRepository invoiceRepository,
            @Value("${invoiceguard.risk.duplicate.near-duplicate-threshold:60}") int nearDuplicateThreshold,
            @Value("${invoiceguard.risk.duplicate.window-days:30}") int nearDuplicateWindowDays,
            @Value("${invoiceguard.risk.duplicate.amount-tolerance-percent:2}") int amountTolerancePercent) {
        this.invoiceRepository = invoiceRepository;
        this.nearDuplicateThreshold = nearDuplicateThreshold;
        this.nearDuplicateWindowDays = nearDuplicateWindowDays;
        this.nearDuplicateAmountTolerancePercent = BigDecimal.valueOf(amountTolerancePercent);
    }

    public List<DuplicateMatch> findExactDuplicates(Invoice invoice) {
        List<Invoice> candidates = invoiceRepository.findByOrganizationIdAndVendorIdAndNormalizedInvoiceNumber(
                invoice.getOrganizationId(), invoice.getVendorId(), invoice.getNormalizedInvoiceNumber());

        List<DuplicateMatch> matches = new ArrayList<>();
        for (Invoice candidate : candidates) {
            if (candidate.getId().equals(invoice.getId())) {
                continue;
            }
            boolean sameAmount = candidate.getTotalAmount().compareTo(invoice.getTotalAmount()) == 0;
            boolean sameDate = candidate.getInvoiceDate().equals(invoice.getInvoiceDate());
            if (sameAmount && sameDate) {
                matches.add(new DuplicateMatch(
                        candidate.getId(),
                        "EXACT",
                        100,
                        List.of("vendor", "normalizedInvoiceNumber", "totalAmount", "invoiceDate"),
                        "Identical vendor, invoice number, amount, and date as invoice " + candidate.getInvoiceNumber(),
                        candidate.getTotalAmount()));
            }
        }
        return matches;
    }

    public List<DuplicateMatch> findNearDuplicates(Invoice invoice) {
        LocalDate from = invoice.getInvoiceDate().minusDays(nearDuplicateWindowDays);
        LocalDate to = invoice.getInvoiceDate().plusDays(nearDuplicateWindowDays);
        List<Invoice> candidates = invoiceRepository.findByOrganizationIdAndVendorIdAndInvoiceDateBetween(
                invoice.getOrganizationId(), invoice.getVendorId(), from, to);

        List<DuplicateMatch> matches = new ArrayList<>();
        for (Invoice candidate : candidates) {
            if (candidate.getId().equals(invoice.getId())) {
                continue;
            }
            int score = scoreNearDuplicate(invoice, candidate);
            if (score >= nearDuplicateThreshold) {
                List<String> matchedFields = new ArrayList<>();
                double numberSimilarity = StringSimilarity.normalizedSimilarity(
                        invoice.getNormalizedInvoiceNumber(), candidate.getNormalizedInvoiceNumber());
                if (numberSimilarity >= 0.8) matchedFields.add("invoiceNumber");
                if (isWithinAmountTolerance(invoice, candidate)) matchedFields.add("totalAmount");
                if (isSimilarPurchaseOrder(invoice, candidate)) matchedFields.add("purchaseOrderNumber");
                if (isSimilarDescription(invoice, candidate)) matchedFields.add("description");

                matches.add(new DuplicateMatch(
                        candidate.getId(),
                        "NEAR",
                        score,
                        matchedFields,
                        "Similar to invoice " + candidate.getInvoiceNumber() + " (" + String.join(", ", matchedFields) + ")",
                        candidate.getTotalAmount()));
            }
        }
        return matches;
    }

    private int scoreNearDuplicate(Invoice invoice, Invoice candidate) {
        double numberSimilarity = StringSimilarity.normalizedSimilarity(
                invoice.getNormalizedInvoiceNumber(), candidate.getNormalizedInvoiceNumber());
        double score = 0;
        score += numberSimilarity * 45; // invoice-number closeness carries the most weight
        if (isWithinAmountTolerance(invoice, candidate)) score += 30;
        if (isSimilarPurchaseOrder(invoice, candidate)) score += 15;
        if (isSimilarDescription(invoice, candidate)) score += 10;
        return (int) Math.min(100, Math.round(score));
    }

    private boolean isWithinAmountTolerance(Invoice a, Invoice b) {
        if (a.getTotalAmount().signum() == 0) {
            return b.getTotalAmount().signum() == 0;
        }
        BigDecimal diff = a.getTotalAmount().subtract(b.getTotalAmount()).abs();
        BigDecimal tolerance = a.getTotalAmount()
                .abs()
                .multiply(nearDuplicateAmountTolerancePercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return diff.compareTo(tolerance) <= 0;
    }

    private boolean isSimilarPurchaseOrder(Invoice a, Invoice b) {
        if (isBlank(a.getPurchaseOrderNumber()) || isBlank(b.getPurchaseOrderNumber())) {
            return false;
        }
        return StringSimilarity.normalizedSimilarity(a.getPurchaseOrderNumber(), b.getPurchaseOrderNumber()) >= 0.85;
    }

    private boolean isSimilarDescription(Invoice a, Invoice b) {
        if (isBlank(a.getDescription()) || isBlank(b.getDescription())) {
            return false;
        }
        return StringSimilarity.wordOverlapSimilarity(a.getDescription(), b.getDescription()) >= 0.5;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
