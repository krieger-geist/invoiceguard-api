package com.invoiceguard.audit.listener;

import com.invoiceguard.approval.event.InvoiceApprovedEvent;
import com.invoiceguard.approval.event.InvoiceRejectedEvent;
import com.invoiceguard.audit.entity.AuditResult;
import com.invoiceguard.audit.service.AuditService;
import com.invoiceguard.auth.event.AccountLockedEvent;
import com.invoiceguard.auth.event.LoginFailedEvent;
import com.invoiceguard.auth.event.LoginSucceededEvent;
import com.invoiceguard.auth.event.UserRegisteredEvent;
import com.invoiceguard.invoice.event.InvoiceSubmittedEvent;
import com.invoiceguard.integration.apikey.ApiKeyGeneratedEvent;
import com.invoiceguard.organization.repository.OrganizationMemberRepository;
import com.invoiceguard.risk.event.InvoiceAnalysedEvent;
import com.invoiceguard.risk.event.RiskRuleConfigChangedEvent;
import com.invoiceguard.vendor.event.VendorBankChangeRequestedEvent;
import com.invoiceguard.vendor.event.VendorCreatedEvent;
import com.invoiceguard.vendor.event.VendorSuspendedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * One handler per event type the spec calls out for audit coverage: login,
 * failed login, vendor creation, bank-detail changes, invoice submission,
 * invoice approval/rejection, and risk-rule changes. Role changes and
 * API-key generation are not yet auditable because those features don't
 * exist yet (member role assignment is a Phase-2 leftover with no update
 * endpoint; API keys are Phase 7) — this listener is the single place to
 * add their handlers once those actions exist.
 */
@Component
public class AuditEventListener {

    private final AuditService auditService;
    private final OrganizationMemberRepository organizationMemberRepository;

    public AuditEventListener(AuditService auditService, OrganizationMemberRepository organizationMemberRepository) {
        this.auditService = auditService;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        auditService.record(
                event.organizationId(), "USER_REGISTERED", "User", event.userId(),
                "{\"email\":\"" + event.email() + "\"}", null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onLoginSucceeded(LoginSucceededEvent event) {
        auditService.record(
                event.organizationId(), "LOGIN_SUCCESS", "User", event.userId(),
                null, event.ipAddress(), AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onLoginFailed(LoginFailedEvent event) {
        auditService.record(
                null, "LOGIN_FAILED", "User", null,
                "{\"email\":\"" + event.email() + "\"}", event.ipAddress(), AuditResult.FAILURE, event.reason());
    }

    @EventListener
    public void onAccountLocked(AccountLockedEvent event) {
        organizationMemberRepository.findByUserId(event.userId()).forEach(membership -> auditService.record(
                membership.getOrganizationId(), "ACCOUNT_LOCKED", "User", event.userId(),
                null, event.ipAddress(), AuditResult.FAILURE, "Repeated failed login attempts"));
    }

    @EventListener
    public void onVendorCreated(VendorCreatedEvent event) {
        auditService.record(
                event.organizationId(), "VENDOR_CREATED", "Vendor", event.vendorId(),
                "{\"vendorCode\":\"" + event.vendorCode() + "\"}", null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onVendorSuspended(VendorSuspendedEvent event) {
        auditService.record(event.organizationId(), "VENDOR_SUSPENDED", "Vendor", event.vendorId(), null, null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onVendorBankChangeRequested(VendorBankChangeRequestedEvent event) {
        auditService.record(
                event.organizationId(), "VENDOR_BANK_CHANGE_REQUESTED", "Vendor", event.vendorId(),
                "{\"bankChangeRequestId\":\"" + event.bankChangeRequestId() + "\"}", null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onInvoiceSubmitted(InvoiceSubmittedEvent event) {
        auditService.record(event.organizationId(), "INVOICE_SUBMITTED", "Invoice", event.invoiceId(), null, null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onInvoiceAnalysed(InvoiceAnalysedEvent event) {
        auditService.record(
                event.organizationId(), "INVOICE_ANALYSED", "Invoice", event.invoiceId(),
                "{\"riskLevel\":\"" + event.riskLevel() + "\",\"score\":" + event.totalRiskScore() + "}",
                null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onInvoiceApproved(InvoiceApprovedEvent event) {
        auditService.record(event.organizationId(), "INVOICE_APPROVED", "Invoice", event.invoiceId(), null, null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onInvoiceRejected(InvoiceRejectedEvent event) {
        auditService.record(
                event.organizationId(), "INVOICE_REJECTED", "Invoice", event.invoiceId(),
                event.reason(), null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onApiKeyGenerated(ApiKeyGeneratedEvent event) {
        auditService.record(
                event.organizationId(), "API_KEY_GENERATED", "ApiKey", event.apiKeyId(),
                "{\"name\":\"" + event.name() + "\"}", null, AuditResult.SUCCESS, null);
    }

    @EventListener
    public void onRiskRuleConfigChanged(RiskRuleConfigChangedEvent event) {
        auditService.record(
                event.organizationId(), "RISK_RULE_CONFIG_CHANGED", "RiskRuleConfiguration", null,
                "{\"ruleCode\":\"" + event.ruleCode() + "\",\"enabled\":" + event.enabled() + ",\"weight\":" + event.weightPoints() + "}",
                null, AuditResult.SUCCESS, null);
    }
}
