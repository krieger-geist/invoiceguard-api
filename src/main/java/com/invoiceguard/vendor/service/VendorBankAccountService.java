package com.invoiceguard.vendor.service;

import com.invoiceguard.exception.ApplicationException;
import com.invoiceguard.exception.BusinessRuleViolationException;
import com.invoiceguard.exception.ErrorCode;
import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.security.BankAccountEncryptionService;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.vendor.dto.BankAccountSubmissionRequest;
import com.invoiceguard.vendor.entity.BankAccountVerificationStatus;
import com.invoiceguard.vendor.entity.BankChangeRequest;
import com.invoiceguard.vendor.entity.BankChangeRequestStatus;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorBankAccount;
import com.invoiceguard.vendor.repository.BankChangeRequestRepository;
import com.invoiceguard.vendor.repository.VendorBankAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the rule that gives this module its name in the spec: a newly
 * submitted bank account never immediately becomes the vendor's payable
 * account. It is created {@code PENDING}/{@code inactive}, paired with a
 * {@link BankChangeRequest}, and only promoted to active once that request
 * is explicitly approved by someone holding {@code vendor:bank-change-approve}
 * (see {@code VendorBankAccountController}).
 */
@Service
public class VendorBankAccountService {

    private final VendorBankAccountRepository bankAccountRepository;
    private final BankChangeRequestRepository bankChangeRequestRepository;
    private final VendorService vendorService;
    private final BankAccountEncryptionService encryptionService;
    private final TenantContext tenantContext;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public VendorBankAccountService(
            VendorBankAccountRepository bankAccountRepository,
            BankChangeRequestRepository bankChangeRequestRepository,
            VendorService vendorService,
            BankAccountEncryptionService encryptionService,
            TenantContext tenantContext,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankChangeRequestRepository = bankChangeRequestRepository;
        this.vendorService = vendorService;
        this.encryptionService = encryptionService;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<VendorBankAccount> listForVendor(UUID vendorId) {
        vendorService.getOwnedById(vendorId); // tenant + existence check
        return bankAccountRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    @Transactional(readOnly = true)
    public List<BankChangeRequest> listChangeRequestsForVendor(UUID vendorId) {
        vendorService.getOwnedById(vendorId);
        return bankChangeRequestRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    @Transactional
    public BankChangeRequest submitBankAccount(UUID vendorId, BankAccountSubmissionRequest request, UUID requestedBy) {
        Vendor vendor = vendorService.getOwnedById(vendorId);
        UUID organizationId = vendor.getOrganizationId();

        if (bankChangeRequestRepository.existsByVendorIdAndStatus(vendorId, BankChangeRequestStatus.PENDING)) {
            throw new BusinessRuleViolationException(
                    "This vendor already has a pending bank-change request; resolve it before submitting another");
        }

        VendorBankAccount previousActive = bankAccountRepository.findByVendorIdAndActiveTrue(vendorId).orElse(null);

        VendorBankAccount newAccount = new VendorBankAccount();
        newAccount.setOrganizationId(organizationId);
        newAccount.setVendorId(vendorId);
        newAccount.setAccountHolderName(request.accountHolderName());
        newAccount.setMaskedAccountNumber(encryptionService.mask(request.accountNumber()));
        newAccount.setEncryptedAccountNumber(encryptionService.encrypt(request.accountNumber()));
        newAccount.setBankName(request.bankName());
        newAccount.setBranchName(request.branchName());
        newAccount.setRoutingCode(request.routingCode());
        newAccount.setAccountType(request.accountType());
        newAccount.setVerificationStatus(BankAccountVerificationStatus.PENDING);
        newAccount.setActive(false);
        newAccount = bankAccountRepository.save(newAccount);

        BankChangeRequest changeRequest = new BankChangeRequest();
        changeRequest.setOrganizationId(organizationId);
        changeRequest.setVendorId(vendorId);
        changeRequest.setRequestedBankAccountId(newAccount.getId());
        changeRequest.setPreviousBankAccountId(previousActive != null ? previousActive.getId() : null);
        changeRequest.setStatus(BankChangeRequestStatus.PENDING);
        changeRequest.setRequestedBy(requestedBy);
        changeRequest.setNotes(request.notes());

        changeRequest = bankChangeRequestRepository.save(changeRequest);
        eventPublisher.publishEvent(new com.invoiceguard.vendor.event.VendorBankChangeRequestedEvent(
                vendorId, organizationId, changeRequest.getId(), requestedBy));
        return changeRequest;
    }

    @Transactional
    public BankChangeRequest decide(UUID vendorId, UUID changeRequestId, boolean approve, String notes, UUID reviewerId) {
        UUID organizationId = tenantContext.requireOrganizationId();

        BankChangeRequest changeRequest = bankChangeRequestRepository
                .findByIdAndOrganizationId(changeRequestId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("BankChangeRequest", changeRequestId));

        if (!changeRequest.getVendorId().equals(vendorId)) {
            throw new ApplicationException(
                    ErrorCode.RESOURCE_NOT_FOUND, "Bank change request does not belong to this vendor");
        }
        if (changeRequest.getStatus() != BankChangeRequestStatus.PENDING) {
            throw new BusinessRuleViolationException("This bank-change request has already been decided");
        }

        VendorBankAccount requestedAccount = bankAccountRepository
                .findByIdAndOrganizationId(changeRequest.getRequestedBankAccountId(), organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "VendorBankAccount", changeRequest.getRequestedBankAccountId()));

        if (approve) {
            bankAccountRepository.findByVendorIdAndActiveTrue(vendorId).ifPresent(current -> {
                current.setActive(false);
                current.setEffectiveTo(Instant.now());
                bankAccountRepository.save(current);
            });

            requestedAccount.setVerificationStatus(BankAccountVerificationStatus.VERIFIED);
            requestedAccount.setActive(true);
            requestedAccount.setEffectiveFrom(Instant.now());
            bankAccountRepository.save(requestedAccount);

            changeRequest.setStatus(BankChangeRequestStatus.APPROVED);
        } else {
            requestedAccount.setVerificationStatus(BankAccountVerificationStatus.REJECTED);
            bankAccountRepository.save(requestedAccount);

            changeRequest.setStatus(BankChangeRequestStatus.REJECTED);
        }

        changeRequest.setReviewedBy(reviewerId);
        changeRequest.setReviewedAt(Instant.now());
        changeRequest.setNotes(notes != null ? notes : changeRequest.getNotes());

        return bankChangeRequestRepository.save(changeRequest);
    }
}
