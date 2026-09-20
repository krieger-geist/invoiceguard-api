package com.invoiceguard.vendor.dto;

import jakarta.validation.constraints.Size;

public record BankChangeDecisionRequest(boolean approve, @Size(max = 1000) String notes) {}
