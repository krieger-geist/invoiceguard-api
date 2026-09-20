package com.invoiceguard.vendor.entity;

/**
 * Current risk classification of a vendor. Defaults to {@code LOW} at
 * creation and is updated by the risk-scoring engine (Phase 5) as invoices
 * for this vendor are analysed — there is no public endpoint to set this
 * directly, only to read it, since it is a derived/computed fact rather
 * than user-entered data.
 */
public enum VendorRiskStatus {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
