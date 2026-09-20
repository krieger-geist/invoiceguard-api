package com.invoiceguard.risk.service;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Non-AI statistical anomaly detection: computes a vendor's historical
 * invoicing profile so rules like {@code UnusualAmountRule} can flag
 * invoices that are statistical outliers for THIS vendor specifically,
 * rather than against some organisation-wide average that wouldn't mean
 * much when vendors range from a $50/month subscription to a $500,000
 * equipment supplier.
 *
 * <p>Uses {@code double} for the statistical computations (mean, standard
 * deviation) even though the underlying amounts are {@code BigDecimal} —
 * these are derived, transient analytics values used only for anomaly
 * scoring, never persisted as financial figures or used in any monetary
 * calculation, so the precision trade-off is appropriate here and nowhere
 * else in the codebase.
 */
@Service
public class VendorStatisticsService {

    private final InvoiceRepository invoiceRepository;

    public VendorStatisticsService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public VendorStatistics computeFor(UUID organizationId, UUID vendorId, UUID excludeInvoiceId) {
        List<Invoice> history = invoiceRepository
                .findByOrganizationIdAndVendorIdOrderByInvoiceDateDesc(organizationId, vendorId)
                .stream()
                .filter(inv -> !inv.getId().equals(excludeInvoiceId))
                .toList();

        if (history.isEmpty()) {
            return VendorStatistics.empty();
        }

        List<BigDecimal> amounts =
                history.stream().map(Invoice::getTotalAmount).sorted().toList();

        double mean = amounts.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = amounts.stream()
                .mapToDouble(a -> Math.pow(a.doubleValue() - mean, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);

        BigDecimal median = amounts.size() % 2 == 1
                ? amounts.get(amounts.size() / 2)
                : amounts.get(amounts.size() / 2 - 1)
                        .add(amounts.get(amounts.size() / 2))
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        double averageTaxRate = history.stream()
                .filter(inv -> inv.getSubtotal().signum() > 0)
                .mapToDouble(inv -> inv.getTaxAmount().doubleValue() / inv.getSubtotal().doubleValue())
                .average()
                .orElse(0);

        Map<DayOfWeek, Long> submissionDayCounts = history.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getCreatedAt().atZone(ZoneOffset.UTC).getDayOfWeek(), Collectors.counting()));
        DayOfWeek mostCommonDay = Collections.max(submissionDayCounts.entrySet(), Map.Entry.comparingByValue())
                .getKey();

        return new VendorStatistics(
                history.size(),
                BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP),
                median,
                stdDev,
                amounts.get(0),
                amounts.get(amounts.size() - 1),
                averageTaxRate,
                mostCommonDay);
    }
}
