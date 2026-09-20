package com.invoiceguard.vendor.repository;

import com.invoiceguard.vendor.entity.VendorBankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorBankAccountRepository extends JpaRepository<VendorBankAccount, UUID> {

    List<VendorBankAccount> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    Optional<VendorBankAccount> findByVendorIdAndActiveTrue(UUID vendorId);

    Optional<VendorBankAccount> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
