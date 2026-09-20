package com.invoiceguard.approval.dto;

import com.invoiceguard.security.Role;
import java.math.BigDecimal;
import java.util.UUID;

public record ApprovalPolicyResponse(
        UUID id, String name, BigDecimal minAmount, BigDecimal maxAmount, int requiredApprovals, Role minimumApproverRole, boolean active) {}
