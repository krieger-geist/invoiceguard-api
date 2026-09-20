package com.invoiceguard.vendor.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A vendor (supplier) an organisation receives invoices from. {@code vendorCode}
 * is unique per organisation (not globally) — two different tenants may both
 * have a vendor coded "V-1001" with no collision, since uniqueness is scoped
 * by the {@code (organization_id, vendor_code)} constraint in the migration.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "vendors",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "vendor_code"}))
public class Vendor extends OrganizationScopedEntity {

    @Column(name = "vendor_code", nullable = false, length = 50)
    private String vendorCode;

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VendorVerificationStatus verificationStatus = VendorVerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_status", nullable = false, length = 30)
    private VendorRiskStatus riskStatus = VendorRiskStatus.LOW;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String displayNameOrLegalName() {
        return (displayName != null && !displayName.isBlank()) ? displayName : legalName;
    }
}
