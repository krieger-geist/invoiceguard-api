package com.invoiceguard.vendor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * A single bank-account record in a vendor's history. Only ever ONE row per
 * vendor should have {@code active = true} at a time — the currently
 * verified, payable account. New submissions are created with
 * {@code active = false} and {@code verificationStatus = PENDING}, and only
 * become active once a {@link BankChangeRequest} referencing them is
 * approved — see {@code VendorBankAccountService.submitBankAccount}.
 *
 * <p>{@code encryptedAccountNumber} is AES-GCM ciphertext
 * (see {@code com.invoiceguard.security.BankAccountEncryptionService}) and is
 * never serialized to JSON ({@code @JsonIgnore}); every API response exposes
 * only {@code maskedAccountNumber}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_bank_accounts")
public class VendorBankAccount extends OrganizationScopedEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "account_holder_name", nullable = false, length = 255)
    private String accountHolderName;

    @Column(name = "masked_account_number", nullable = false, length = 50)
    private String maskedAccountNumber;

    @JsonIgnore
    @Column(name = "encrypted_account_number", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccountNumber;

    @Column(name = "bank_name", nullable = false, length = 255)
    private String bankName;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "routing_code", nullable = false, length = 50)
    private String routingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private BankAccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private BankAccountVerificationStatus verificationStatus = BankAccountVerificationStatus.PENDING;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "active", nullable = false)
    private boolean active = false;
}
