package com.invoiceguard.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorUpdateRequest(
        @NotBlank @Size(max = 255) String legalName,
        @Size(max = 255) String displayName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 500) String address,
        @NotBlank @Size(min = 2, max = 2) String country,
        @Size(max = 50) String taxNumber,
        @Size(max = 50) String gstNumber,
        boolean active) {}
