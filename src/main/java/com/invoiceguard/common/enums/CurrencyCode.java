package com.invoiceguard.common.enums;

/**
 * Supported ISO-4217 currency codes. Not exhaustive — covers the currencies
 * an initial deployment is likely to invoice in. Adding a currency is a
 * one-line enum addition; nothing else in the codebase needs to change,
 * since every money field is typed {@code BigDecimal} with a currency
 * carried alongside it rather than assumed.
 */
public enum CurrencyCode {
    USD,
    EUR,
    GBP,
    INR,
    AUD,
    CAD,
    JPY,
    CNY,
    SGD,
    AED,
    CHF,
    ZAR,
    NZD,
    HKD,
    SEK
}
