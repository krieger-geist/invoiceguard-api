package com.invoiceguard.approval.repository;

import com.invoiceguard.approval.entity.ApprovalRequest;
import com.invoiceguard.approval.entity.ApprovalRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    Optional<ApprovalRequest> findByInvoiceId(UUID invoiceId);

    Optional<ApprovalRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ApprovalRequest> findByStatusAndCreatedAtBefore(ApprovalRequestStatus status, Instant threshold);
}
