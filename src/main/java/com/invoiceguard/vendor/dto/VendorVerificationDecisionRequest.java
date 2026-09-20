package com.invoiceguard.vendor.dto;

import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Target status must be one of VERIFIED, REJECTED, SUSPENDED — see VendorService's transition map. */
public record VendorVerificationDecisionRequest(
        @NotNull VendorVerificationStatus targetStatus, @Size(max = 1000) String notes) {}
