package com.invoiceguard.approval.listener;

import com.invoiceguard.approval.service.ApprovalService;
import com.invoiceguard.invoice.service.InvoiceService;
import com.invoiceguard.risk.event.InvoiceAnalysedEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * Event-driven bridge between the risk engine and the approval workflow —
 * per the spec, these modules communicate via Spring Application Events
 * rather than a direct service dependency, so the risk module has no
 * compile-time knowledge that an approval module even exists.
 *
 * <p>Uses {@code AFTER_COMMIT} deliberately: {@code RiskEngine.analyse()}
 * publishes this event from inside its own still-open transaction, before
 * the invoice's new status/risk-level are durably written. Reacting
 * immediately (or via a naive {@code REQUIRES_NEW}) would query the invoice
 * from a separate connection that can't yet see those uncommitted changes.
 * Waiting for commit guarantees this listener always sees the invoice
 * exactly as the risk engine left it.
 */
@Component
public class ApprovalRequestCreationListener {

    private final ApprovalService approvalService;
    private final InvoiceService invoiceService;

    public ApprovalRequestCreationListener(ApprovalService approvalService, InvoiceService invoiceService) {
        this.approvalService = approvalService;
        this.invoiceService = invoiceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoiceAnalysed(InvoiceAnalysedEvent event) {
        approvalService.createRequest(invoiceService.getOwnedById(event.invoiceId()));
    }
}

