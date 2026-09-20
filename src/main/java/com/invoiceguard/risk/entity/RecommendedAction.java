package com.invoiceguard.risk.entity;

/** What the engine suggests a human do next. The engine never acts on this itself — see RiskEngine javadoc. */
public enum RecommendedAction {
    AUTO_APPROVE_ELIGIBLE,
    STANDARD_REVIEW,
    MANUAL_REVIEW,
    BLOCK_PAYMENT,
    REQUEST_VENDOR_VERIFICATION
}
