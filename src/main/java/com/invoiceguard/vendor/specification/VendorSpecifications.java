package com.invoiceguard.vendor.specification;

import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorRiskStatus;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification} predicates for vendor search. Every
 * caller MUST include {@link #belongsToOrganization} — this is the
 * repository-level enforcement half of tenant isolation, complementing
 * {@code TenantContext} at the service layer (see {@code VendorService}).
 */
public final class VendorSpecifications {

    private VendorSpecifications() {}

    public static Specification<Vendor> belongsToOrganization(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Vendor> hasVerificationStatus(VendorVerificationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("verificationStatus"), status);
    }

    public static Specification<Vendor> hasRiskStatus(VendorRiskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("riskStatus"), status);
    }

    public static Specification<Vendor> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    public static Specification<Vendor> hasCountry(String country) {
        return (root, query, cb) -> (country == null || country.isBlank())
                ? null
                : cb.equal(cb.upper(root.get("country")), country.toUpperCase());
    }

    public static Specification<Vendor> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate byLegalName = cb.like(cb.lower(root.get("legalName")), pattern);
            Predicate byDisplayName = cb.like(cb.lower(root.get("displayName")), pattern);
            Predicate byVendorCode = cb.like(cb.lower(root.get("vendorCode")), pattern);
            return cb.or(byLegalName, byDisplayName, byVendorCode);
        };
    }
}
