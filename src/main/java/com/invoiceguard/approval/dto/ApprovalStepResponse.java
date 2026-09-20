package com.invoiceguard.approval.dto;

import com.invoiceguard.approval.entity.ApprovalStepStatus;
import java.time.Instant;
import java.util.UUID;

public record ApprovalStepResponse(
        UUID id, int sequenceNumber, ApprovalStepStatus status, UUID decidedBy, Instant decidedAt, String comments) {}
