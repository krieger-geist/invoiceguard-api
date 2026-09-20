package com.invoiceguard.audit.dto;

import com.invoiceguard.audit.entity.AuditResult;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String action,
        String entityType,
        UUID entityId,
        String oldValues,
        String newValues,
        String ipAddress,
        String correlationId,
        AuditResult result,
        String failureReason,
        String performedBy,
        Instant occurredAt) {}
