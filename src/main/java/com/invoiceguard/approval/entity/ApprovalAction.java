package com.invoiceguard.approval.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable history record of one action against an {@link ApprovalRequest}.
 * Rows are append-only — {@code createdAt}/{@code createdBy} (inherited)
 * are "when" and "who" for free, matching the pattern already used by
 * {@code InvoiceStatusHistory}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_actions")
public class ApprovalAction extends OrganizationScopedEntity {

    @Column(name = "approval_request_id", nullable = false)
    private UUID approvalRequestId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ApprovalActionType actionType;

    @Column(name = "comments", length = 1000)
    private String comments;
}
