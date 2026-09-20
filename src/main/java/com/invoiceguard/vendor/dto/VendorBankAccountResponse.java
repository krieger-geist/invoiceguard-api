package com.invoiceguard.vendor.dto;

import com.invoiceguard.vendor.entity.BankAccountType;
import com.invoiceguard.vendor.entity.BankAccountVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorBankAccountResponse(
        UUID id,
        UUID vendorId,
        String accountHolderName,
        String maskedAccountNumber,
        String bankName,
        String branchName,
        String routingCode,
        BankAccountType accountType,
        BankAccountVerificationStatus verificationStatus,
        boolean active,
        Instant effectiveFrom,
        Instant effectiveTo,
        Instant createdAt) {}
