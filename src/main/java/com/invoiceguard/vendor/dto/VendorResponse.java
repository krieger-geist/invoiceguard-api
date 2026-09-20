package com.invoiceguard.vendor.dto;

import com.invoiceguard.vendor.entity.VendorRiskStatus;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        String vendorCode,
        String legalName,
        String displayName,
        String email,
        String phone,
        String address,
        String country,
        String taxNumber,
        String gstNumber,
        VendorVerificationStatus verificationStatus,
        VendorRiskStatus riskStatus,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
