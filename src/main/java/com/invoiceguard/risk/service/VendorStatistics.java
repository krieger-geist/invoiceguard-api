package com.invoiceguard.risk.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * A vendor's historical invoicing profile, computed fresh on each analysis
 * run (not cached/stored — cheap to compute and always reflects the latest
 * data). {@code sampleSize} tells rules whether the statistics are reliable
 * enough to act on; see {@code RiskEngineProperties.minStatisticalSampleSize}.
 */
public record VendorStatistics(
        int sampleSize,
        BigDecimal averageAmount,
        BigDecimal medianAmount,
        double stdDevAmount,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        double averageTaxRate,
        DayOfWeek mostCommonSubmissionDay) {

    public static VendorStatistics empty() {
        return new VendorStatistics(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, null);
    }
}
