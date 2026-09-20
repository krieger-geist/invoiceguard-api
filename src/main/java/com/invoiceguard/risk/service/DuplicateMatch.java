package com.invoiceguard.risk.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One candidate duplicate found for an invoice being analysed.
 * {@code confidenceScore} is 0-100; {@code matchType} of {@code "EXACT"} is
 * always 100. Not a persisted entity — findings derived from these are what
 * get persisted, as {@code RiskFinding} rows with this data serialized into
 * {@code evidence}.
 */
public record DuplicateMatch(
        UUID matchedInvoiceId,
        String matchType,
        int confidenceScore,
        List<String> matchedFields,
        String explanation,
        BigDecimal matchedInvoiceAmount) {}
