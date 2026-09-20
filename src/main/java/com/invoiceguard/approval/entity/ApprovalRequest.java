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
 * The overall approval case for one invoice. {@code requiredApprovals} is
 * frozen at creation time (computed by {@code ApprovalPolicyService} from
 * the policy in effect then) — later policy changes never retroactively
 * change an in-flight request's requirement.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest extends OrganizationScopedEntity {

    @Column(name = "invoice_id", nullable = false, unique = true)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalRequestStatus status = ApprovalRequestStatus.PENDING;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    @Column(name = "approvals_received", nullable = false)
    private int approvalsReceived = 0;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;
}
