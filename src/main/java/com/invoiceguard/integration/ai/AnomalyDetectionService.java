package com.invoiceguard.integration.ai;

import com.invoiceguard.risk.service.VendorStatistics;
import java.math.BigDecimal;

/**
 * Abstraction over "is this amount anomalous for this vendor". Backed today
 * by {@code DefaultAnomalyDetectionService}, which wraps the same z-score
 * logic {@code UnusualAmountRule} uses against {@code VendorStatistics} — a
 * future ML-based anomaly model (trained across more signals than amount
 * alone) could implement this interface instead with no change to callers.
 */
public interface AnomalyDetectionService {

    boolean isAnomalous(BigDecimal amount, VendorStatistics statistics, double zScoreThreshold);
}
