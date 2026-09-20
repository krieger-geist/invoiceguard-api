package com.invoiceguard.integration.webhook;

import com.invoiceguard.alert.event.AlertCreatedEvent;
import com.invoiceguard.approval.event.InvoiceApprovedEvent;
import com.invoiceguard.approval.event.InvoiceRejectedEvent;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.event.InvoiceSubmittedEvent;
import com.invoiceguard.risk.event.InvoiceAnalysedEvent;
import com.invoiceguard.vendor.event.VendorBankChangeRequestedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates the same domain events {@code AlertEventListener} and
 * {@code AuditEventListener} consume into the dot-namespaced webhook event
 * names from the spec ({@code invoice.submitted}, {@code invoice.approved}, ...).
 * All three listeners subscribing to the same underlying events — rather
 * than webhooks being bolted onto the alert or audit listener — is
 * deliberate: alerts, audit, and webhooks are three independent consumers
 * of the same facts, and none of them should know the others exist.
 */
@Component
public class WebhookDispatchListener {

    private final WebhookDispatchService dispatchService;

    public WebhookDispatchListener(WebhookDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @EventListener
    public void onInvoiceSubmitted(InvoiceSubmittedEvent event) {
        dispatchService.dispatch(event.organizationId(), "invoice.submitted", Map.of("invoiceId", event.invoiceId().toString()));
    }

    @EventListener
    public void onInvoiceAnalysed(InvoiceAnalysedEvent event) {
        dispatchService.dispatch(
                event.organizationId(),
                "invoice.risk.completed",
                Map.of("invoiceId", event.invoiceId().toString(), "riskLevel", event.riskLevel().toString(), "score", event.totalRiskScore()));

        if (event.riskLevel() == InvoiceRiskLevel.HIGH || event.riskLevel() == InvoiceRiskLevel.CRITICAL) {
            dispatchService.dispatch(
                    event.organizationId(), "invoice.review.required", Map.of("invoiceId", event.invoiceId().toString()));
        }
    }

    @EventListener
    public void onInvoiceApproved(InvoiceApprovedEvent event) {
        dispatchService.dispatch(event.organizationId(), "invoice.approved", Map.of("invoiceId", event.invoiceId().toString()));
    }

    @EventListener
    public void onInvoiceRejected(InvoiceRejectedEvent event) {
        dispatchService.dispatch(event.organizationId(), "invoice.rejected", Map.of("invoiceId", event.invoiceId().toString()));
    }

    @EventListener
    public void onVendorBankChangeRequested(VendorBankChangeRequestedEvent event) {
        dispatchService.dispatch(event.organizationId(), "vendor.bank.changed", Map.of("vendorId", event.vendorId().toString()));
    }

    @EventListener
    public void onAlertCreated(AlertCreatedEvent event) {
        dispatchService.dispatch(
                event.organizationId(), "alert.created",
                Map.of("alertId", event.alertId().toString(), "alertType", event.alertType(), "severity", event.severity()));
    }
}
