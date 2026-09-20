package com.invoiceguard.vendor.dto;

import com.invoiceguard.vendor.entity.BankAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankAccountSubmissionRequest(
        @NotBlank @Size(max = 255) String accountHolderName,
        @NotBlank @Pattern(regexp = "^[0-9A-Za-z]{4,34}$", message = "Account number must be 4-34 alphanumeric characters")
                String accountNumber,
        @NotBlank @Size(max = 255) String bankName,
        @Size(max = 255) String branchName,
        @NotBlank @Size(max = 50) String routingCode,
        @NotNull BankAccountType accountType,
        @Size(max = 1000) String notes) {}
