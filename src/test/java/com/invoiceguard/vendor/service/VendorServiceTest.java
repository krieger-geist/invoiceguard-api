package com.invoiceguard.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.invoiceguard.exception.InvalidStateTransitionException;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import com.invoiceguard.vendor.repository.VendorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock private VendorRepository vendorRepository;
    @Mock private TenantContext tenantContext;
    @Mock private ApplicationEventPublisher eventPublisher;

    private VendorService vendorService;

    private final UUID orgId = UUID.randomUUID();
    private final UUID vendorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        vendorService = new VendorService(vendorRepository, tenantContext, eventPublisher);
    }

    private Vendor vendorWithStatus(VendorVerificationStatus status) {
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setOrganizationId(orgId);
        vendor.setVerificationStatus(status);
        return vendor;
    }

    @Test
    void pendingVendorCanBeVerified() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendorWithStatus(VendorVerificationStatus.PENDING)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor result = vendorService.changeVerificationStatus(vendorId, VendorVerificationStatus.VERIFIED);

        assertThat(result.getVerificationStatus()).isEqualTo(VendorVerificationStatus.VERIFIED);
    }

    @Test
    void pendingVendorCanBeRejected() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendorWithStatus(VendorVerificationStatus.PENDING)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor result = vendorService.changeVerificationStatus(vendorId, VendorVerificationStatus.REJECTED);

        assertThat(result.getVerificationStatus()).isEqualTo(VendorVerificationStatus.REJECTED);
    }

    @Test
    void cannotJumpDirectlyFromPendingToSuspended() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendorWithStatus(VendorVerificationStatus.PENDING)));

        assertThatThrownBy(() -> vendorService.changeVerificationStatus(vendorId, VendorVerificationStatus.SUSPENDED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void verifiedVendorCanBeSuspendedAndReactivated() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendorWithStatus(VendorVerificationStatus.VERIFIED)));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor suspended = vendorService.changeVerificationStatus(vendorId, VendorVerificationStatus.SUSPENDED);
        assertThat(suspended.getVerificationStatus()).isEqualTo(VendorVerificationStatus.SUSPENDED);
    }

    @Test
    void suspendedVendorCannotJumpToRejected() {
        when(tenantContext.requireOrganizationId()).thenReturn(orgId);
        when(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendorWithStatus(VendorVerificationStatus.SUSPENDED)));

        assertThatThrownBy(() -> vendorService.changeVerificationStatus(vendorId, VendorVerificationStatus.REJECTED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
