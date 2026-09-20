package com.invoiceguard.alert.listener;

import com.invoiceguard.alert.entity.AlertType;
import com.invoiceguard.alert.service.AlertService;
import com.invoiceguard.auth.event.AccountLockedEvent;
import com.invoiceguard.organization.repository.OrganizationMemberRepository;
import com.invoiceguard.risk.entity.RecommendedAction;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.event.CriticalRiskDetectedEvent;
import com.invoiceguard.risk.event.InvoiceAnalysedEvent;
import com.invoiceguard.vendor.event.VendorBankChangeRequestedEvent;
import com.invoiceguard.vendor.event.VendorSuspendedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates domain events raised across the app into {@link com.invoiceguard.alert.entity.Alert}
 * rows. This is the module that gives the event-driven design in the spec
 * (InvoiceAnalysedEvent, CriticalRiskDetectedEvent, VendorBankChangeRequestedEvent, ...)
 * an actual consumer — every event listed in the spec's Section 12 examples
 * has a handler here.
 *
 * <p>Uses plain {@code @EventListener} (synchronous, same transaction) rather
 * than {@code @TransactionalEventListener(AFTER_COMMIT)}: unlike
 * {@code ApprovalRequestCreationListener}, these handlers only read data
 * already present on the event payload itself, never re-querying an entity
 * whose commit visibility would matter.
 */
@Component
public class AlertEventListener {

    private final AlertService alertService;
    private final OrganizationMemberRepository organizationMemberRepository;

    public AlertEventListener(AlertService alertService, OrganizationMemberRepository organizationMemberRepository) {
        this.alertService = alertService;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @EventListener
    public void onCriticalRiskDetected(CriticalRiskDetectedEvent event) {
        alertService.raise(
                event.organizationId(),
                AlertType.CRITICAL_RISK_INVOICE,
                RiskFindingSeverity.CRITICAL,
                "Critical-risk invoice detected",
                "Invoice scored " + event.totalRiskScore() + "/100 and requires manual verification before payment.",
                "Invoice",
                event.invoiceId());
    }

    @EventListener
    public void onInvoiceAnalysed(InvoiceAnalysedEvent event) {
        if (event.hasExactDuplicate()) {
            alertService.raise(
                    event.organizationId(),
                    AlertType.DUPLICATE_INVOICE,
                    RiskFindingSeverity.HIGH,
                    "Duplicate invoice detected",
                    "This invoice appears to be an exact duplicate of a previously submitted invoice.",
                    "Invoice",
                    event.invoiceId());
        }
        if (event.recommendedAction() == RecommendedAction.BLOCK_PAYMENT) {
            alertService.raise(
                    event.organizationId(),
                    AlertType.PAYMENT_BLOCKED,
                    RiskFindingSeverity.CRITICAL,
                    "Payment blocked pending review",
                    "The risk engine recommends blocking payment on this invoice until it is manually reviewed.",
                    "Invoice",
                    event.invoiceId());
        }
    }

    @EventListener
    public void onVendorBankChangeRequested(VendorBankChangeRequestedEvent event) {
        alertService.raise(
                event.organizationId(),
                AlertType.VENDOR_BANK_CHANGED,
                RiskFindingSeverity.MEDIUM,
                "Vendor bank details changed",
                "A new bank account was submitted for this vendor and is pending approval.",
                "Vendor",
                event.vendorId());
    }

    @EventListener
    public void onVendorSuspended(VendorSuspendedEvent event) {
        alertService.raise(
                event.organizationId(),
                AlertType.VENDOR_SUSPENDED,
                RiskFindingSeverity.HIGH,
                "Vendor suspended",
                "This vendor's verification status was changed to SUSPENDED.",
                "Vendor",
                event.vendorId());
    }

    @EventListener
    public void onAccountLocked(AccountLockedEvent event) {
        organizationMemberRepository.findByUserId(event.userId()).forEach(membership -> alertService.raise(
                membership.getOrganizationId(),
                AlertType.REPEATED_LOGIN_FAILURE,
                RiskFindingSeverity.MEDIUM,
                "Repeated failed login attempts",
                "The account for " + event.email() + " was locked after repeated failed login attempts.",
                "User",
                event.userId()));
    }
}
