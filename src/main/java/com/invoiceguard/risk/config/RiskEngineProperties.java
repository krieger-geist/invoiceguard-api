package com.invoiceguard.risk.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Every numeric/list threshold the risk rules use, bound from
 * {@code invoiceguard.risk.*}. Centralizing these here (rather than
 * scattering {@code @Value} across 21 rule classes) is what makes "make the
 * thresholds configurable" from the spec actually mean something — an
 * operator can retune the whole engine's sensitivity via environment
 * variables alone, with no code change or redeploy.
 */
@ConfigurationProperties(prefix = "invoiceguard.risk")
public record RiskEngineProperties(
        Thresholds thresholds,
        int minStatisticalSampleSize,
        double unusualAmountZScoreThreshold,
        double taxMismatchTolerancePercentagePoints,
        int oldInvoiceDateDays,
        int businessHourStart,
        int businessHourEnd,
        int recentBankChangeWindowDays,
        int vendorRecentlyCreatedDays,
        int rapidRepeatWindowMinutes,
        List<String> highRiskCountries,
        List<String> freeEmailDomains) {

    /** Score-band cutoffs: LOW is 0..lowMax, MEDIUM is lowMax+1..mediumMax, etc. Anything above highMax is CRITICAL. */
    public record Thresholds(int lowMax, int mediumMax, int highMax) {}
}
