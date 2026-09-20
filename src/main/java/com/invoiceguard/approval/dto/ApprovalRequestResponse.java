package com.invoiceguard.approval.dto;

import com.invoiceguard.approval.entity.ApprovalRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalRequestResponse(
        UUID id,
        UUID invoiceId,
        ApprovalRequestStatus status,
        int requiredApprovals,
        int approvalsReceived,
        UUID submittedBy,
        Instant decidedAt,
        List<ApprovalStepResponse> steps) {}
