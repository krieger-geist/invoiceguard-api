package com.invoiceguard.approval.service;

import com.invoiceguard.approval.entity.ApprovalAction;
import com.invoiceguard.approval.entity.ApprovalActionType;
import com.invoiceguard.approval.entity.ApprovalRequest;
import com.invoiceguard.approval.entity.ApprovalRequestStatus;
import com.invoiceguard.approval.entity.ApprovalStep;
import com.invoiceguard.approval.entity.ApprovalStepStatus;
import com.invoiceguard.approval.event.InvoiceApprovedEvent;
import com.invoiceguard.approval.event.InvoiceRejectedEvent;
import com.invoiceguard.approval.repository.ApprovalActionRepository;
import com.invoiceguard.approval.repository.ApprovalRequestRepository;
import com.invoiceguard.approval.repository.ApprovalStepRepository;
import com.invoiceguard.exception.ApplicationException;
import com.invoiceguard.exception.BusinessRuleViolationException;
import com.invoiceguard.exception.ErrorCode;
import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import com.invoiceguard.invoice.service.InvoiceService;
import com.invoiceguard.security.AuthenticatedPrincipal;
import com.invoiceguard.security.Permission;
import com.invoiceguard.security.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approve/reject workflow for invoices. An {@link ApprovalRequest} is
 * created automatically once risk analysis completes (see
 * {@code ApprovalRequestCreationListener}, triggered by
 * {@code InvoiceAnalysedEvent}) — this service is where a human's decision
 * is recorded against that request.
 *
 * <p>Two rules enforced here that don't fit neatly into
 * {@code @PreAuthorize} because they're data-dependent, not purely
 * role-dependent:
 * <ul>
 *   <li>Segregation of duties — the invoice's creator can never approve it,
 *       regardless of role.</li>
 *   <li>Risk-gated approval — {@code HIGH}-risk invoices additionally require
 *       {@code invoice:approve-high-risk}; {@code CRITICAL}-risk invoices
 *       additionally require {@code invoice:approve-critical-risk}. Per
 *       {@code RolePermissions}, only {@code FINANCE_MANAGER} and
 *       {@code ORGANIZATION_ADMIN} hold the critical-risk permission — this
 *       is the concrete mechanism behind "critical risk invoices require
 *       finance-manager approval" and "block until manual verification."</li>
 * </ul>
 */
@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final ApprovalPolicyService approvalPolicyService;
    private final InvoiceService invoiceService;
    private final TenantContext tenantContext;
    private final ApplicationEventPublisher eventPublisher;

    public ApprovalService(
            ApprovalRequestRepository approvalRequestRepository,
            ApprovalStepRepository approvalStepRepository,
            ApprovalActionRepository approvalActionRepository,
            ApprovalPolicyService approvalPolicyService,
            InvoiceService invoiceService,
            TenantContext tenantContext,
            ApplicationEventPublisher eventPublisher) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.approvalActionRepository = approvalActionRepository;
        this.approvalPolicyService = approvalPolicyService;
        this.invoiceService = invoiceService;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
    }

    /** Called by {@code ApprovalRequestCreationListener} once an invoice finishes risk analysis. */
    @Transactional
    public ApprovalRequest createRequest(Invoice invoice) {
        int requiredApprovals = approvalPolicyService.resolveRequiredApprovals(invoice);

        ApprovalRequest request = new ApprovalRequest();
        request.setOrganizationId(invoice.getOrganizationId());
        request.setInvoiceId(invoice.getId());
        request.setStatus(ApprovalRequestStatus.PENDING);
        request.setRequiredApprovals(requiredApprovals);
        request.setApprovalsReceived(0);
        request = approvalRequestRepository.save(request);

        for (int i = 1; i <= requiredApprovals; i++) {
            ApprovalStep step = new ApprovalStep();
            step.setOrganizationId(invoice.getOrganizationId());
            step.setApprovalRequestId(request.getId());
            step.setInvoiceId(invoice.getId());
            step.setSequenceNumber(i);
            step.setStatus(ApprovalStepStatus.PENDING);
            approvalStepRepository.save(step);
        }

        recordAction(request, invoice.getId(), ApprovalActionType.SUBMITTED, "Approval request created");
        return request;
    }

    @Transactional
    public ApprovalRequest approve(UUID invoiceId, String comments) {
        UUID organizationId = tenantContext.requireOrganizationId();
        Invoice invoice = invoiceService.getOwnedById(invoiceId);
        ApprovalRequest request = getPendingRequest(invoiceId, organizationId);

        AuthenticatedPrincipal principal = tenantContext
                .currentPrincipal()
                .orElseThrow(() -> new ApplicationException(ErrorCode.AUTHENTICATION_FAILED, "No authenticated user"));

        assertNotSelfApproval(invoice, principal);
        assertRiskPermission(invoice, principal);

        if (approvalStepRepository.existsByApprovalRequestIdAndDecidedByAndStatus(
                request.getId(), principal.userId(), ApprovalStepStatus.APPROVED)) {
            throw new BusinessRuleViolationException("You have already approved this invoice");
        }

        ApprovalStep step = approvalStepRepository
                .findFirstByApprovalRequestIdAndStatusOrderBySequenceNumberAsc(request.getId(), ApprovalStepStatus.PENDING)
                .orElseThrow(() -> new BusinessRuleViolationException("No pending approval steps remain on this request"));

        step.setStatus(ApprovalStepStatus.APPROVED);
        step.setDecidedBy(principal.userId());
        step.setDecidedAt(Instant.now());
        step.setComments(comments);
        approvalStepRepository.save(step);

        request.setApprovalsReceived(request.getApprovalsReceived() + 1);
        recordAction(request, invoiceId, ApprovalActionType.APPROVED, comments);

        if (request.getApprovalsReceived() >= request.getRequiredApprovals()) {
            request.setStatus(ApprovalRequestStatus.APPROVED);
            request.setDecidedAt(Instant.now());
            invoiceService.transition(invoiceId, InvoiceStatus.APPROVED, "Approved via approval workflow");
            eventPublisher.publishEvent(new InvoiceApprovedEvent(invoiceId, organizationId, principal.userId()));
        }

        return approvalRequestRepository.save(request);
    }

    @Transactional
    public ApprovalRequest reject(UUID invoiceId, String comments) {
        UUID organizationId = tenantContext.requireOrganizationId();
        invoiceService.getOwnedById(invoiceId); // tenant + existence check
        ApprovalRequest request = getPendingRequest(invoiceId, organizationId);
        UUID actorId = tenantContext.requireUserId();

        List<ApprovalStep> steps = approvalStepRepository.findByApprovalRequestIdOrderBySequenceNumberAsc(request.getId());
        boolean decidingStepHandled = false;
        for (ApprovalStep step : steps) {
            if (step.getStatus() != ApprovalStepStatus.PENDING) {
                continue;
            }
            if (!decidingStepHandled) {
                step.setStatus(ApprovalStepStatus.REJECTED);
                step.setDecidedBy(actorId);
                step.setDecidedAt(Instant.now());
                step.setComments(comments);
                decidingStepHandled = true;
            } else {
                step.setStatus(ApprovalStepStatus.CANCELLED);
            }
            approvalStepRepository.save(step);
        }

        request.setStatus(ApprovalRequestStatus.REJECTED);
        request.setDecidedAt(Instant.now());
        recordAction(request, invoiceId, ApprovalActionType.REJECTED, comments);

        invoiceService.transition(invoiceId, InvoiceStatus.REJECTED, comments != null ? comments : "Rejected via approval workflow");
        eventPublisher.publishEvent(new InvoiceRejectedEvent(invoiceId, organizationId, actorId, comments));

        return approvalRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public ApprovalRequest getForInvoice(UUID invoiceId) {
        UUID organizationId = tenantContext.requireOrganizationId();
        return approvalRequestRepository
                .findByInvoiceId(invoiceId)
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("No approval request exists for this invoice"));
    }

    @Transactional(readOnly = true)
    public List<ApprovalStep> getSteps(UUID approvalRequestId) {
        return approvalStepRepository.findByApprovalRequestIdOrderBySequenceNumberAsc(approvalRequestId);
    }

    private ApprovalRequest getPendingRequest(UUID invoiceId, UUID organizationId) {
        ApprovalRequest request = approvalRequestRepository
                .findByInvoiceId(invoiceId)
                .filter(r -> r.getOrganizationId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("No approval request exists for this invoice"));
        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new BusinessRuleViolationException("This approval request has already been decided (" + request.getStatus() + ")");
        }
        return request;
    }

    private void assertNotSelfApproval(Invoice invoice, AuthenticatedPrincipal principal) {
        if (invoice.getCreatedBy() != null && invoice.getCreatedBy().equalsIgnoreCase(principal.email())) {
            throw new ApplicationException(ErrorCode.SELF_APPROVAL_NOT_ALLOWED, "You cannot approve an invoice you submitted yourself");
        }
    }

    private void assertRiskPermission(Invoice invoice, AuthenticatedPrincipal principal) {
        if (invoice.getRiskLevel() == InvoiceRiskLevel.CRITICAL && !principal.hasPermission(Permission.INVOICE_APPROVE_CRITICAL_RISK)) {
            throw new ApplicationException(
                    ErrorCode.APPROVAL_NOT_ALLOWED, "Critical-risk invoices require finance-manager (or higher) approval");
        }
        if (invoice.getRiskLevel() == InvoiceRiskLevel.HIGH && !principal.hasPermission(Permission.INVOICE_APPROVE_HIGH_RISK)) {
            throw new ApplicationException(
                    ErrorCode.APPROVAL_NOT_ALLOWED, "High-risk invoices require an approver with elevated approval permission");
        }
    }

    private void recordAction(ApprovalRequest request, UUID invoiceId, ApprovalActionType type, String comments) {
        ApprovalAction action = new ApprovalAction();
        action.setOrganizationId(request.getOrganizationId());
        action.setApprovalRequestId(request.getId());
        action.setInvoiceId(invoiceId);
        action.setActionType(type);
        action.setComments(comments);
        approvalActionRepository.save(action);
    }
}
