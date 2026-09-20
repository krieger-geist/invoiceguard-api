package com.invoiceguard.alert.dto;

import com.invoiceguard.alert.entity.AlertStatus;
import com.invoiceguard.alert.entity.AlertType;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        AlertType type,
        AlertStatus status,
        RiskFindingSeverity severity,
        String title,
        String message,
        String relatedEntityType,
        UUID relatedEntityId,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant createdAt) {}
