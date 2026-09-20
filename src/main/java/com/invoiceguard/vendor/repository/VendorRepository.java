package com.invoiceguard.vendor.repository;

import com.invoiceguard.vendor.entity.Vendor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VendorRepository extends JpaRepository<Vendor, UUID>, JpaSpecificationExecutor<Vendor> {

    Optional<Vendor> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndVendorCode(UUID organizationId, String vendorCode);
}
