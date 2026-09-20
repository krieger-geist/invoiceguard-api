package com.invoiceguard.approval.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import com.invoiceguard.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable approval tier, e.g. "invoices under 25,000 need 1 reviewer",
 * "25,000-100,000 need a finance manager", "above 100,000 need 2 reviewers".
 * {@code ApprovalPolicyService} picks the matching policy by amount range;
 * an organisation with no policies configured falls back to a single-approval
 * default, so approval works out of the box before any admin sets this up.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_policies")
public class ApprovalPolicy extends OrganizationScopedEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "min_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minAmount;

    /** Null means "no upper bound". */
    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    /** If set, at least one approver on the request must hold this role or higher. Advisory metadata only in v1 — not yet hard-enforced per-step. */
    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_approver_role", length = 30)
    private Role minimumApproverRole;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
