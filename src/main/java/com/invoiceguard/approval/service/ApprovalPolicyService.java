package com.invoiceguard.approval.service;

import com.invoiceguard.approval.dto.ApprovalPolicyRequest;
import com.invoiceguard.approval.dto.ApprovalPolicyResponse;
import com.invoiceguard.approval.entity.ApprovalPolicy;
import com.invoiceguard.approval.repository.ApprovalPolicyRepository;
import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.security.TenantContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns "how many approvals does this invoice need" into a concrete number,
 * combining the organisation's configured amount-tier {@link ApprovalPolicy}
 * with a risk-level bump for {@code HIGH}-risk invoices (the spec's
 * "high risk requires an additional risk analyst" rule). {@code CRITICAL}
 * risk is handled separately, as a permission gate at approval time
 * (only {@code invoice:approve-critical-risk} holders — finance managers and
 * org admins — can approve at all) rather than as an extra count, which is
 * what "block until manual verification" means in practice.
 */
@Service
public class ApprovalPolicyService {

    private static final int DEFAULT_REQUIRED_APPROVALS = 1;

    private final ApprovalPolicyRepository repository;
    private final TenantContext tenantContext;

    public ApprovalPolicyService(ApprovalPolicyRepository repository, TenantContext tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public int resolveRequiredApprovals(Invoice invoice) {
        List<ApprovalPolicy> policies =
                repository.findByOrganizationIdAndActiveTrueOrderByMinAmountAsc(invoice.getOrganizationId());

        int required = policies.stream()
                .filter(p -> invoice.getTotalAmount().compareTo(p.getMinAmount()) >= 0)
                .filter(p -> p.getMaxAmount() == null || invoice.getTotalAmount().compareTo(p.getMaxAmount()) < 0)
                .reduce((first, second) -> second) // last matching (highest minAmount) tier wins
                .map(ApprovalPolicy::getRequiredApprovals)
                .orElse(DEFAULT_REQUIRED_APPROVALS);

        if (invoice.getRiskLevel() == InvoiceRiskLevel.HIGH) {
            required = Math.max(required, required + 1);
        }
        return required;
    }

    @Transactional(readOnly = true)
    public List<ApprovalPolicyResponse> listAll() {
        UUID organizationId = tenantContext.requireOrganizationId();
        return repository.findByOrganizationIdOrderByMinAmountAsc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ApprovalPolicyResponse create(ApprovalPolicyRequest request) {
        UUID organizationId = tenantContext.requireOrganizationId();
        ApprovalPolicy policy = new ApprovalPolicy();
        policy.setOrganizationId(organizationId);
        apply(policy, request);
        return toResponse(repository.save(policy));
    }

    @Transactional
    public ApprovalPolicyResponse update(UUID policyId, ApprovalPolicyRequest request) {
        UUID organizationId = tenantContext.requireOrganizationId();
        ApprovalPolicy policy = repository
                .findByIdAndOrganizationId(policyId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("ApprovalPolicy", policyId));
        apply(policy, request);
        return toResponse(repository.save(policy));
    }

    private void apply(ApprovalPolicy policy, ApprovalPolicyRequest request) {
        policy.setName(request.name());
        policy.setMinAmount(request.minAmount());
        policy.setMaxAmount(request.maxAmount());
        policy.setRequiredApprovals(request.requiredApprovals());
        policy.setMinimumApproverRole(request.minimumApproverRole());
        policy.setActive(request.active());
    }

    private ApprovalPolicyResponse toResponse(ApprovalPolicy policy) {
        return new ApprovalPolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getMinAmount(),
                policy.getMaxAmount(),
                policy.getRequiredApprovals(),
                policy.getMinimumApproverRole(),
                policy.isActive());
    }
}
