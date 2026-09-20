package com.invoiceguard.approval.dto;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(@Size(max = 1000) String comments) {}
