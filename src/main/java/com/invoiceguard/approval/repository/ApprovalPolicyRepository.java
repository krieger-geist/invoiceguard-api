package com.invoiceguard.approval.repository;

import com.invoiceguard.approval.entity.ApprovalPolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy, UUID> {

    List<ApprovalPolicy> findByOrganizationIdAndActiveTrueOrderByMinAmountAsc(UUID organizationId);

    List<ApprovalPolicy> findByOrganizationIdOrderByMinAmountAsc(UUID organizationId);

    java.util.Optional<ApprovalPolicy> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
