package com.invoiceguard.vendor.dto;

import com.invoiceguard.vendor.entity.BankChangeRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record BankChangeRequestResponse(
        UUID id,
        UUID vendorId,
        UUID requestedBankAccountId,
        UUID previousBankAccountId,
        BankChangeRequestStatus status,
        UUID requestedBy,
        UUID reviewedBy,
        Instant reviewedAt,
        String notes,
        Instant createdAt) {}
