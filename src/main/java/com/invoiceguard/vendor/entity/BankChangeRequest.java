package com.invoiceguard.vendor.entity;

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
 * Tracks the approval of a newly submitted {@link VendorBankAccount}. This is
 * the entity that makes "bank-detail changes require separate approval" an
 * enforced workflow rather than a policy statement: submitting a new bank
 * account never immediately makes it payable — only approving the
 * corresponding {@code BankChangeRequest} does.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bank_change_requests")
public class BankChangeRequest extends OrganizationScopedEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "requested_bank_account_id", nullable = false)
    private UUID requestedBankAccountId;

    @Column(name = "previous_bank_account_id")
    private UUID previousBankAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BankChangeRequestStatus status = BankChangeRequestStatus.PENDING;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "notes", length = 1000)
    private String notes;
}
