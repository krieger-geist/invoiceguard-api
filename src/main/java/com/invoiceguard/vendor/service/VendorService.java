package com.invoiceguard.vendor.service;

import com.invoiceguard.exception.DuplicateResourceException;
import com.invoiceguard.exception.InvalidStateTransitionException;
import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.vendor.dto.VendorCreateRequest;
import com.invoiceguard.vendor.dto.VendorUpdateRequest;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorRiskStatus;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import com.invoiceguard.vendor.repository.VendorRepository;
import com.invoiceguard.vendor.specification.VendorSpecifications;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    /** Legal verification-status transitions. Anything not listed here is rejected. */
    private static final Map<VendorVerificationStatus, Set<VendorVerificationStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(VendorVerificationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                VendorVerificationStatus.PENDING,
                EnumSet.of(VendorVerificationStatus.VERIFIED, VendorVerificationStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(VendorVerificationStatus.VERIFIED, EnumSet.of(VendorVerificationStatus.SUSPENDED));
        ALLOWED_TRANSITIONS.put(VendorVerificationStatus.SUSPENDED, EnumSet.of(VendorVerificationStatus.VERIFIED));
        ALLOWED_TRANSITIONS.put(VendorVerificationStatus.REJECTED, EnumSet.of(VendorVerificationStatus.PENDING));
    }

    private final VendorRepository vendorRepository;
    private final TenantContext tenantContext;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public VendorService(
            VendorRepository vendorRepository,
            TenantContext tenantContext,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.vendorRepository = vendorRepository;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Vendor create(VendorCreateRequest request) {
        UUID organizationId = tenantContext.requireOrganizationId();

        String vendorCode = (request.vendorCode() == null || request.vendorCode().isBlank())
                ? generateVendorCode(organizationId)
                : request.vendorCode().trim().toUpperCase();

        if (vendorRepository.existsByOrganizationIdAndVendorCode(organizationId, vendorCode)) {
            throw new DuplicateResourceException("A vendor with code '" + vendorCode + "' already exists");
        }

        Vendor vendor = new Vendor();
        vendor.setOrganizationId(organizationId);
        vendor.setVendorCode(vendorCode);
        vendor.setLegalName(request.legalName());
        vendor.setDisplayName(request.displayName());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setAddress(request.address());
        vendor.setCountry(request.country().toUpperCase());
        vendor.setTaxNumber(request.taxNumber());
        vendor.setGstNumber(request.gstNumber());
        vendor.setVerificationStatus(VendorVerificationStatus.PENDING);
        vendor.setRiskStatus(VendorRiskStatus.LOW);
        vendor.setActive(true);

        vendor = vendorRepository.save(vendor);
        eventPublisher.publishEvent(new com.invoiceguard.vendor.event.VendorCreatedEvent(vendor.getId(), organizationId, vendor.getVendorCode()));
        return vendor;
    }

    @Transactional
    public Vendor update(UUID vendorId, VendorUpdateRequest request) {
        Vendor vendor = getOwnedById(vendorId);
        vendor.setLegalName(request.legalName());
        vendor.setDisplayName(request.displayName());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setAddress(request.address());
        vendor.setCountry(request.country().toUpperCase());
        vendor.setTaxNumber(request.taxNumber());
        vendor.setGstNumber(request.gstNumber());
        vendor.setActive(request.active());
        return vendorRepository.save(vendor);
    }

    @Transactional
    public Vendor changeVerificationStatus(UUID vendorId, VendorVerificationStatus targetStatus) {
        Vendor vendor = getOwnedById(vendorId);
        Set<VendorVerificationStatus> allowed =
                ALLOWED_TRANSITIONS.getOrDefault(vendor.getVerificationStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw InvalidStateTransitionException.of("Vendor", vendor.getVerificationStatus(), targetStatus);
        }
        vendor.setVerificationStatus(targetStatus);
        vendor = vendorRepository.save(vendor);
        if (targetStatus == VendorVerificationStatus.SUSPENDED) {
            eventPublisher.publishEvent(new com.invoiceguard.vendor.event.VendorSuspendedEvent(vendor.getId(), vendor.getOrganizationId()));
        }
        return vendor;
    }

    /**
     * Sets the vendor's computed risk classification. Not exposed via any
     * controller — this is written only by {@code RiskEngine} after
     * analysing an invoice, since risk status is a derived fact, not
     * something a user directly edits (see {@code VendorRiskStatus} javadoc).
     */
    @Transactional
    public void updateRiskStatus(UUID vendorId, VendorRiskStatus riskStatus) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow(() -> ResourceNotFoundException.of("Vendor", vendorId));
        vendor.setRiskStatus(riskStatus);
        vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    public Vendor getOwnedById(UUID vendorId) {
        UUID organizationId = tenantContext.requireOrganizationId();
        return vendorRepository
                .findByIdAndOrganizationId(vendorId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vendor", vendorId));
    }

    @Transactional(readOnly = true)
    public Page<Vendor> search(
            VendorVerificationStatus verificationStatus,
            VendorRiskStatus riskStatus,
            Boolean active,
            String country,
            String keyword,
            Pageable pageable) {
        UUID organizationId = tenantContext.requireOrganizationId();
        Specification<Vendor> spec = Specification.where(VendorSpecifications.belongsToOrganization(organizationId))
                .and(VendorSpecifications.hasVerificationStatus(verificationStatus))
                .and(VendorSpecifications.hasRiskStatus(riskStatus))
                .and(VendorSpecifications.isActive(active))
                .and(VendorSpecifications.hasCountry(country))
                .and(VendorSpecifications.keyword(keyword));
        return vendorRepository.findAll(spec, pageable);
    }

    private String generateVendorCode(UUID organizationId) {
        String candidate;
        do {
            candidate = "VEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (vendorRepository.existsByOrganizationIdAndVendorCode(organizationId, candidate));
        return candidate;
    }
}