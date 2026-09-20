package com.invoiceguard.risk.event;

import java.util.UUID;

/** Published in addition to InvoiceAnalysedEvent whenever an assessment lands in CRITICAL. Consumed by the alert module (Phase 6). */
public record CriticalRiskDetectedEvent(UUID invoiceId, UUID organizationId, int totalRiskScore) {}
