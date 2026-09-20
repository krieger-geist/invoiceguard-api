package com.invoiceguard.approval.repository;

import com.invoiceguard.approval.entity.ApprovalAction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, UUID> {

    List<ApprovalAction> findByApprovalRequestIdOrderByCreatedAtAsc(UUID approvalRequestId);
}
