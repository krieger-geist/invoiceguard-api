package com.invoiceguard.vendor.repository;

import com.invoiceguard.vendor.entity.BankChangeRequest;
import com.invoiceguard.vendor.entity.BankChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankChangeRequestRepository extends JpaRepository<BankChangeRequest, UUID> {

    List<BankChangeRequest> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    Optional<BankChangeRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByVendorIdAndStatus(UUID vendorId, BankChangeRequestStatus status);
}
