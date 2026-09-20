package com.invoiceguard.approval.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One of the N approval "slots" a request needs filled. Slots are created
 * unassigned (an open pool any eligible approver can claim by approving) —
 * there's no pre-assignment/routing to a specific person in v1, which keeps
 * the model simple while still enforcing the required-count and one-approval-
 * per-person rules. Named routing/delegation is a natural future addition.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_steps")
public class ApprovalStep extends OrganizationScopedEntity {

    @Column(name = "approval_request_id", nullable = false)
    private UUID approvalRequestId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "comments", length = 1000)
    private String comments;
}
