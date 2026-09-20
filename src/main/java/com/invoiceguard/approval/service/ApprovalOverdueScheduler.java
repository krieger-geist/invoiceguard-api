package com.invoiceguard.approval.service;

import com.invoiceguard.alert.entity.AlertType;
import com.invoiceguard.alert.service.AlertService;
import com.invoiceguard.approval.entity.ApprovalRequest;
import com.invoiceguard.approval.entity.ApprovalRequestStatus;
import com.invoiceguard.approval.repository.ApprovalRequestRepository;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically checks for {@link ApprovalRequest}s that have sat PENDING
 * longer than {@code invoiceguard.approval.overdue-hours} and raises an
 * {@code APPROVAL_OVERDUE} alert for each — {@link AlertService#raise}'s
 * built-in deduplication means this runs safely on a schedule without
 * spamming a duplicate alert for the same still-overdue request every run.
 */
@Component
public class ApprovalOverdueScheduler {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AlertService alertService;
    private final int overdueHours;

    public ApprovalOverdueScheduler(
            ApprovalRequestRepository approvalRequestRepository,
            AlertService alertService,
            @Value("${invoiceguard.approval.overdue-hours:48}") int overdueHours) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.alertService = alertService;
        this.overdueHours = overdueHours;
    }

    @Scheduled(fixedDelayString = "${invoiceguard.approval.overdue-check-interval-ms:3600000}")
    public void checkOverdueApprovals() {
        Instant threshold = Instant.now().minus(overdueHours, ChronoUnit.HOURS);
        for (ApprovalRequest request :
                approvalRequestRepository.findByStatusAndCreatedAtBefore(ApprovalRequestStatus.PENDING, threshold)) {
            alertService.raise(
                    request.getOrganizationId(),
                    AlertType.APPROVAL_OVERDUE,
                    RiskFindingSeverity.MEDIUM,
                    "Approval overdue",
                    "An invoice has been pending approval for more than " + overdueHours + " hours.",
                    "Invoice",
                    request.getInvoiceId());
        }
    }
}
