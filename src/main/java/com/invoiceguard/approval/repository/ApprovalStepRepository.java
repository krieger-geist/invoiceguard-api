package com.invoiceguard.approval.repository;

import com.invoiceguard.approval.entity.ApprovalStep;
import com.invoiceguard.approval.entity.ApprovalStepStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, UUID> {

    List<ApprovalStep> findByApprovalRequestIdOrderBySequenceNumberAsc(UUID approvalRequestId);

    Optional<ApprovalStep> findFirstByApprovalRequestIdAndStatusOrderBySequenceNumberAsc(
            UUID approvalRequestId, ApprovalStepStatus status);

    boolean existsByApprovalRequestIdAndDecidedByAndStatus(UUID approvalRequestId, UUID decidedBy, ApprovalStepStatus status);
}
