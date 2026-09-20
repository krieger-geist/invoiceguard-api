package com.invoiceguard.approval.dto;

import com.invoiceguard.security.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ApprovalPolicyRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.0") BigDecimal minAmount,
        BigDecimal maxAmount,
        @Min(1) int requiredApprovals,
        Role minimumApproverRole,
        boolean active) {}
